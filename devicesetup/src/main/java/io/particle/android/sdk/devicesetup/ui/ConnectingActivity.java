package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.squareup.phrase.Phrase;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ApConnector;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.setupsteps.CheckIfDeviceClaimedStep;
import io.particle.android.sdk.devicesetup.setupsteps.ConfigureAPStep;
import io.particle.android.sdk.devicesetup.setupsteps.ConnectDeviceToNetworkStep;
import io.particle.android.sdk.devicesetup.setupsteps.EnsureSoftApNotVisible;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStep;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepApReconnector;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsFactory;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsRunnerTask;
import io.particle.android.sdk.devicesetup.setupsteps.StepProgress;
import io.particle.android.sdk.devicesetup.setupsteps.WaitForCloudConnectivityStep;
import io.particle.android.sdk.devicesetup.setupsteps.WaitForDisconnectionFromDeviceStep;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.CoreNameGenerator;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.Funcy;
import io.particle.android.sdk.utils.Py;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.set;
import static io.particle.android.sdk.utils.Py.truthy;


public class ConnectingActivity extends RequiresWifiScansActivity {

    public static final String
            EXTRA_NETWORK_TO_CONFIGURE = "EXTRA_NETWORK_TO_CONFIGURE",
            EXTRA_NETWORK_SECRET = "EXTRA_NETWORK_SECRET",
            EXTRA_SOFT_AP_SSID = "EXTRA_SOFT_AP_SSID";

    private static final TLog log = TLog.get(ConnectingActivity.class);

    public static Intent buildIntent(Context ctx, SSID deviceSoftApSsid,
                                     ScanApCommand.Scan networkToConnectTo) {
        return new Intent(ctx, ConnectingActivity.class)
                .putExtra(EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                        .getApplicationComponent().getGson().toJson(networkToConnectTo))
                .putExtra(EXTRA_SOFT_AP_SSID, deviceSoftApSsid);
    }

    public static Intent buildIntent(Context ctx, SSID deviceSoftApSsid,
                                     ScanApCommand.Scan networkToConnectTo, String secret) {
        return buildIntent(ctx, deviceSoftApSsid, networkToConnectTo)
                .putExtra(EXTRA_NETWORK_SECRET, secret);
    }

    // FIXME: all this state needs to be configured and encapsulated better
    private ConnectingProcessWorkerTask connectingProcessWorkerTask;
    @Inject protected SoftAPConfigRemover softAPConfigRemover;
    @Inject protected WifiFacade wifiFacade;
    protected ApConnector apConnector;
    @Inject protected CommandClientFactory commandClientFactory;
    @Inject protected SetupStepsFactory setupStepsFactory;

    private ScanApCommand.Scan networkToConnectTo;
    private String networkSecretPlaintext;
    private PublicKey publicKey;
    private SSID deviceSoftApSsid;
    @Inject protected ParticleCloud sparkCloud;
    @Inject protected Gson gson;
    private String deviceId;
    private boolean needToClaimDevice;

    private Drawable tintedSpinner;
    private Drawable tintedCheckmark;

    @OnClick(R2.id.action_cancel)
    protected void onCancelClick() {
        connectingProcessWorkerTask.cancel(false);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        ButterKnife.bind(this);
        SEGAnalytics.screen("Device Setup: Connecting progress screen");
        publicKey = DeviceSetupState.publicKey;
        deviceId = DeviceSetupState.deviceToBeSetUpId;
        needToClaimDevice = DeviceSetupState.deviceNeedsToBeClaimed;
        deviceSoftApSsid = getIntent().getParcelableExtra(EXTRA_SOFT_AP_SSID);
        apConnector = new ApConnector(this, softAPConfigRemover, wifiFacade);

        String asJson = getIntent().getStringExtra(EXTRA_NETWORK_TO_CONFIGURE);
        networkToConnectTo = gson.fromJson(asJson, ScanApCommand.Scan.class);
        networkSecretPlaintext = getIntent().getStringExtra(EXTRA_NETWORK_SECRET);

        log.d("Connecting to " + networkToConnectTo + ", with networkSecretPlaintext of size: "
                + ((networkSecretPlaintext == null) ? 0 : networkSecretPlaintext.length()));

        Ui.setText(this, R.id.network_name, networkToConnectTo.ssid);
        Ui.setText(this, R.id.connecting_text,
                Phrase.from(this, R.string.connecting_text)
                        .put("device_name", getString(R.string.device_name))
                        .format()
        );
        Ui.setText(this, R.id.network_name, networkToConnectTo.ssid);

        // FIXME: look into a more elegant way of tinting this stuff.
        tintedSpinner = Ui.getTintedDrawable(ConnectingActivity.this,
                R.drawable.progress_spinner,
//                R.color.element_background_color);
                R.color.element_text_color);
        tintedCheckmark = Ui.getTintedDrawable(ConnectingActivity.this,
                R.drawable.checkmark,
//                R.color.element_background_color);
                R.color.element_text_color);

        connectingProcessWorkerTask = new ConnectingProcessWorkerTask(buildSteps(), 15);
        connectingProcessWorkerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (connectingProcessWorkerTask != null && !connectingProcessWorkerTask.isCancelled()) {
            connectingProcessWorkerTask.cancel(true);
            connectingProcessWorkerTask = null;
        }
        apConnector.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();
    }

    private List<SetupStep> buildSteps() {
        CommandClient commandClient = commandClientFactory.newClientUsingDefaultsForDevices(
                wifiFacade, deviceSoftApSsid);
        SetupStepApReconnector reconnector = new SetupStepApReconnector(
                wifiFacade, apConnector, new Handler(), deviceSoftApSsid);

        ConfigureAPStep configureAPStep = setupStepsFactory.newConfigureApStep(commandClient,
                reconnector, networkToConnectTo, networkSecretPlaintext, publicKey);

        ConnectDeviceToNetworkStep connectDeviceToNetworkStep = setupStepsFactory
                .newConnectDeviceToNetworkStep(commandClient, reconnector);

        WaitForDisconnectionFromDeviceStep waitForDisconnectionFromDeviceStep = setupStepsFactory
                .newWaitForDisconnectionFromDeviceStep(deviceSoftApSsid, wifiFacade);

        EnsureSoftApNotVisible ensureSoftApNotVisible = setupStepsFactory
                .newEnsureSoftApNotVisible(deviceSoftApSsid, wifiFacade);

        WaitForCloudConnectivityStep waitForLocalCloudConnectivityStep = setupStepsFactory
                .newWaitForCloudConnectivityStep(sparkCloud, getApplicationContext());

        CheckIfDeviceClaimedStep checkIfDeviceClaimedStep = setupStepsFactory
                .newCheckIfDeviceClaimedStep(sparkCloud, deviceId, needToClaimDevice);

        List<SetupStep> steps = list(
                configureAPStep,
                connectDeviceToNetworkStep,
                waitForDisconnectionFromDeviceStep,
                ensureSoftApNotVisible,
                waitForLocalCloudConnectivityStep);
        if (!BaseActivity.setupOnly) {
            steps.add(checkIfDeviceClaimedStep);
        }
        return steps;
    }


    private class ConnectingProcessWorkerTask extends SetupStepsRunnerTask {

        ConnectingProcessWorkerTask(List<SetupStep> steps, int maxOverallAttempts) {
            super(steps, maxOverallAttempts);
        }

        @Override
        protected void onProgressUpdate(StepProgress... values) {
            for (StepProgress progress : values) {
                View v = findViewById(progress.stepId);
                if (v != null) {
                    updateProgress(progress, v);
                }
            }
        }

        @Override
        protected void onPostExecute(SetupProcessException error) {
            int resultCode;

            if (error != null) {
                resultCode = error.failedStep.getStepConfig().resultCode;

            } else {
                log.d("HUZZAH, VICTORY!");
                // FIXME: handle "success, no ownership" case
                resultCode = SuccessActivity.RESULT_SUCCESS;

                EZ.runAsync(() -> {
                    try {
                        // collect a list of unique, non-null device names
                        Set<String> names = set(Funcy.transformList(
                                sparkCloud.getDevices(),
                                Funcy.notNull(),
                                ParticleDevice::getName,
                                Py::truthy
                        ));
                        ParticleDevice device = sparkCloud.getDevice(deviceId);
                        if (device != null && !truthy(device.getName())) {
                            device.setName(CoreNameGenerator.generateUniqueName(names));
                        }
                    } catch (Exception e) {
                        // FIXME: do real error handling here, and only
                        // handle ParticleCloudException instead of swallowing everything
                        e.printStackTrace();
                    }
                });
            }

            startActivity(SuccessActivity.buildIntent(ConnectingActivity.this, resultCode, deviceId));
            finish();
        }

        private void updateProgress(StepProgress progress, View progressStepContainer) {
            ProgressBar progBar = Ui.findView(progressStepContainer, R.id.spinner);
            ImageView checkmark = Ui.findView(progressStepContainer, R.id.checkbox);

            // don't show the spinner again if we've already shown the checkmark,
            // regardless of the underlying state that might hide
            if (checkmark.getVisibility() == View.VISIBLE) {
                return;
            }

            progressStepContainer.setVisibility(View.VISIBLE);

            if (progress.status == StepProgress.STARTING) {
                checkmark.setVisibility(View.GONE);

                progBar.setProgressDrawable(tintedSpinner);
                progBar.setVisibility(View.VISIBLE);

            } else {
                progBar.setVisibility(View.GONE);

                checkmark.setImageDrawable(tintedCheckmark);
                checkmark.setVisibility(View.VISIBLE);
            }
        }
    }

}
