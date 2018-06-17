package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.squareup.phrase.Phrase;

import java.security.PublicKey;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.devicesetup.ApConnector;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
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
import io.particle.android.sdk.devicesetup.setupsteps.WaitForCloudConnectivityStep;
import io.particle.android.sdk.devicesetup.setupsteps.WaitForDisconnectionFromDeviceStep;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.ui.BaseFragment;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.list;

public class ConnectingFragment extends BaseFragment {

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
    @Inject protected ApConnector apConnector;
    @Inject protected CommandClientFactory commandClientFactory;
    @Inject protected SetupStepsFactory setupStepsFactory;

    private ScanApCommand.Scan networkToConnectTo;
    private String networkSecretPlaintext;
    private PublicKey publicKey;
    private SSID deviceSoftApSsid;
    @Inject protected ParticleCloud sparkCloud;
    @Inject protected Gson gson;

    @OnClick(R2.id.action_cancel)
    protected void onCancelClick() {
        if (connectingProcessWorkerTask != null && !connectingProcessWorkerTask.isCancelled()) {
            connectingProcessWorkerTask.cancel(false);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_connecting, container, false);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        ButterKnife.bind(this, view);
        SEGAnalytics.screen("Device Setup: Connecting progress screen");
        publicKey = DeviceSetupState.publicKey;
        deviceSoftApSsid = getActivity().getIntent().getParcelableExtra(EXTRA_SOFT_AP_SSID);

        String asJson = getActivity().getIntent().getStringExtra(EXTRA_NETWORK_TO_CONFIGURE);
        networkToConnectTo = gson.fromJson(asJson, ScanApCommand.Scan.class);
        networkSecretPlaintext = getActivity().getIntent().getStringExtra(EXTRA_NETWORK_SECRET);

        log.d("Connecting to " + networkToConnectTo + ", with networkSecretPlaintext of size: "
                + ((networkSecretPlaintext == null) ? 0 : networkSecretPlaintext.length()));

        Ui.setText(view, R.id.network_name, networkToConnectTo.ssid);
        Ui.setText(view, R.id.connecting_text,
                Phrase.from(getActivity(), R.string.connecting_text)
                        .put("device_name", getString(R.string.device_name))
                        .format()
        );
        Ui.setText(view, R.id.network_name, networkToConnectTo.ssid);

        connectingProcessWorkerTask = new ConnectingProcessWorkerTask(getActivity(), buildSteps(), 15);
        connectingProcessWorkerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (connectingProcessWorkerTask != null && !connectingProcessWorkerTask.isCancelled()) {
            connectingProcessWorkerTask.cancel(true);
            connectingProcessWorkerTask = null;
        }
        apConnector.stop();
    }

    @Override
    public void onDestroy() {
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
                .newWaitForCloudConnectivityStep(getContext().getApplicationContext());

        CheckIfDeviceClaimedStep checkIfDeviceClaimedStep = setupStepsFactory
                .newCheckIfDeviceClaimedStep(sparkCloud, DeviceSetupState.deviceToBeSetUpId);

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
}
