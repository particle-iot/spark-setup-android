package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.squareup.phrase.Phrase;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ApConnector;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.setupsteps.CheckIfDeviceClaimedStep;
import io.particle.android.sdk.devicesetup.setupsteps.ConfigureAPStep;
import io.particle.android.sdk.devicesetup.setupsteps.ConnectDeviceToNetworkStep;
import io.particle.android.sdk.devicesetup.setupsteps.EnsureSoftApNotVisible;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStep;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepApReconnector;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsRunnerTask;
import io.particle.android.sdk.devicesetup.setupsteps.StepConfig;
import io.particle.android.sdk.devicesetup.setupsteps.StepProgress;
import io.particle.android.sdk.devicesetup.setupsteps.WaitForCloudConnectivityStep;
import io.particle.android.sdk.devicesetup.setupsteps.WaitForDisconnectionFromDeviceStep;
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

    private static final int
            MAX_RETRIES_CONFIGURE_AP = 5,
            MAX_RETRIES_CONNECT_AP = 5,
            MAX_RETRIES_DISCONNECT_FROM_DEVICE = 5,
            MAX_RETRIES_CLAIM = 5;

    private static final TLog log = TLog.get(ConnectingActivity.class);
    private static final Gson gson = new Gson();


    public static Intent buildIntent(Context ctx, SSID deviceSoftApSsid,
                                     ScanApCommand.Scan networkToConnectTo) {
        return new Intent(ctx, ConnectingActivity.class)
                .putExtra(EXTRA_NETWORK_TO_CONFIGURE, gson.toJson(networkToConnectTo))
                .putExtra(EXTRA_SOFT_AP_SSID, deviceSoftApSsid);
    }


    public static Intent buildIntent(Context ctx, SSID deviceSoftApSsid,
                                     ScanApCommand.Scan networkToConnectTo, String secret) {
        return buildIntent(ctx, deviceSoftApSsid, networkToConnectTo)
                .putExtra(EXTRA_NETWORK_SECRET, secret);
    }


    // FIXME: all this state needs to be configured and encapsulated better
    private ConnectingProcessWorkerTask connectingProcessWorkerTask;
    private SoftAPConfigRemover softAPConfigRemover;
    private ApConnector apConnector;

    private ScanApCommand.Scan networkToConnectTo;
    private String networkSecretPlaintext;
    private PublicKey publicKey;
    private SSID deviceSoftApSsid;
    private ParticleCloud sparkCloud;
    private String deviceId;
    private boolean needToClaimDevice;

    private Drawable tintedSpinner;
    private Drawable tintedCheckmark;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting);
        SEGAnalytics.screen("Device Setup: Connecting progress screen");
        sparkCloud = ParticleCloudSDK.getCloud();
        publicKey = DeviceSetupState.publicKey;
        deviceId = DeviceSetupState.deviceToBeSetUpId;
        needToClaimDevice = DeviceSetupState.deviceNeedsToBeClaimed;
        deviceSoftApSsid = getIntent().getParcelableExtra(EXTRA_SOFT_AP_SSID);

        String asJson = getIntent().getStringExtra(EXTRA_NETWORK_TO_CONFIGURE);
        networkToConnectTo = gson.fromJson(asJson, ScanApCommand.Scan.class);
        networkSecretPlaintext = getIntent().getStringExtra(EXTRA_NETWORK_SECRET);

        log.d("Connecting to " + networkToConnectTo + ", with networkSecretPlaintext of size: "
                + ((networkSecretPlaintext == null) ? 0 : networkSecretPlaintext.length()));

        softAPConfigRemover = new SoftAPConfigRemover(this);
        apConnector = new ApConnector(this);

        Ui.setText(this, R.id.network_name, networkToConnectTo.ssid);
        Button cancelButton = Ui.findView(this, R.id.action_cancel);
        cancelButton.setOnClickListener(v -> {
            if (connectingProcessWorkerTask != null) {
                connectingProcessWorkerTask.cancel(false);
            }
            finish();
        });

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
        CommandClient commandClient = CommandClient.newClientUsingDefaultsForDevices(
                this, deviceSoftApSsid);
        SetupStepApReconnector reconnector = new SetupStepApReconnector(
                WifiFacade.get(this), apConnector, new Handler(), deviceSoftApSsid);

        ConfigureAPStep configureAPStep = new ConfigureAPStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CONFIGURE_AP)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.configure_device_wifi_credentials)
                        .build(),
                commandClient, reconnector, networkToConnectTo, networkSecretPlaintext, publicKey);

        ConnectDeviceToNetworkStep connectDeviceToNetworkStep = new ConnectDeviceToNetworkStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CONNECT_AP)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.connect_to_wifi_network)
                        .build(),
                commandClient, reconnector);

        WaitForDisconnectionFromDeviceStep waitForDisconnectionFromDeviceStep = new WaitForDisconnectionFromDeviceStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_NO_DISCONNECT)
                        .setStepId(R.id.reconnect_to_wifi_network)
                        .build(),
                deviceSoftApSsid, this);

        EnsureSoftApNotVisible ensureSoftApNotVisible = new EnsureSoftApNotVisible(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.wait_for_device_cloud_connection)
                        .build(),
                deviceSoftApSsid, this);

        WaitForCloudConnectivityStep waitForLocalCloudConnectivityStep = new WaitForCloudConnectivityStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_NO_DISCONNECT)
                        .setStepId(R.id.check_for_internet_connectivity)
                        .build(),
                sparkCloud, getApplicationContext());

        CheckIfDeviceClaimedStep checkIfDeviceClaimedStep = new CheckIfDeviceClaimedStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CLAIM)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_CLAIMING)
                        .setStepId(R.id.verify_product_ownership)
                        .build(),
                sparkCloud, deviceId, needToClaimDevice);

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
