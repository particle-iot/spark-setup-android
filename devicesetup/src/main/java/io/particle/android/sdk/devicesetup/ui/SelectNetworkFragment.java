package io.particle.android.sdk.devicesetup.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Set;

import javax.inject.Inject;

import androidx.navigation.Navigation;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.devicesetup.loaders.ScanApCommandLoader;
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;

public class SelectNetworkFragment extends RequiresWifiScansFragment
        implements WifiListFragment.Client<ScanAPCommandResult> {

    public static final String EXTRA_SOFT_AP = "deviceSoftAP";

    private WifiListFragment wifiListFragment;
    @Inject protected WifiFacade wifiFacade;
    @Inject protected CommandClientFactory commandClientFactory;
    private SSID softApSSID;

    @OnClick(R2.id.action_rescan)
    protected void onRescanClick() {
        ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_rescan, true);
        wifiListFragment.scanAsync();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        SEGAnalytics.screen("Device Setup: Select Network Screen");

        softApSSID = getArguments().getParcelable(EXTRA_SOFT_AP);

        View view = inflater.inflate(R.layout.activity_select_network, container, false);
        ButterKnife.bind(this, view);

        wifiListFragment = Ui.findFrag(this, R.id.wifi_list_fragment);
        return view;
    }

    public void onManualNetworkEntryClicked(View view) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ManualNetworkEntryFragment.EXTRA_SOFT_AP, softApSSID);
        Navigation.findNavController(getView()).navigate(R.id.action_selectNetworkFragment_to_manualNetworkEntryFragment, bundle);
    }

    @Override
    public void onNetworkSelected(ScanAPCommandResult selectedNetwork) {
        if (WifiSecurity.isEnterpriseNetwork(selectedNetwork.scan.wifiSecurityType)) {
            new AlertDialog.Builder(getContext())
                    .setMessage(getString(R.string.enterprise_networks_not_supported))
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }
        wifiListFragment.stopAggroLoading();

        if (selectedNetwork.isSecured()) {
            SEGAnalytics.track("Device Setup: Selected secured network");

            Bundle bundle = new Bundle();
            bundle.putParcelable(PasswordEntryFragment.EXTRA_SOFT_AP_SSID, softApSSID);
            bundle.putString(PasswordEntryFragment.EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                    .getApplicationComponent().getGson().toJson(selectedNetwork.scan));
            Navigation.findNavController(getView()).navigate(R.id.action_selectNetworkFragment_to_passwordEntryFragment, bundle);
        } else {
            SEGAnalytics.track("Device Setup: Selected open network");
            SSID softApSSID = wifiFacade.getCurrentlyConnectedSSID();

            Bundle bundle = new Bundle();
            bundle.putParcelable(ConnectingFragment.EXTRA_SOFT_AP_SSID, softApSSID);
            bundle.putString(ConnectingFragment.EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                    .getApplicationComponent().getGson().toJson(selectedNetwork.scan));
            Navigation.findNavController(getView()).navigate(R.id.action_selectNetworkFragment_to_connectingFragment, bundle);
        }
    }

    @Override
    public Loader<Set<ScanAPCommandResult>> createLoader(int id, Bundle args) {
        // FIXME: make the address below use resources instead of hardcoding
        CommandClient client = commandClientFactory.newClientUsingDefaultsForDevices(wifiFacade, softApSSID);
        return new ScanApCommandLoader(getContext(), client);
    }

    @Override
    public void onLoadFinished() {
        ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_rescan, false);
    }

    @Override
    public String getListEmptyText() {
        return getString(R.string.no_wifi_networks_found);
    }

    @Override
    public int getAggroLoadingTimeMillis() {
        return 10000;
    }

}
