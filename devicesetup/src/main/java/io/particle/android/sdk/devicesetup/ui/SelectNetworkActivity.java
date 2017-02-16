package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.View;

import java.util.Set;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.devicesetup.loaders.ScanApCommandLoader;
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;


public class SelectNetworkActivity extends RequiresWifiScansActivity
        implements WifiListFragment.Client<ScanAPCommandResult> {

    private static final String EXTRA_SOFT_AP = "EXTRA_SOFT_AP";


    public static Intent buildIntent(Context ctx, SSID deviceSoftAP) {
        return new Intent(ctx, SelectNetworkActivity.class)
                .putExtra(EXTRA_SOFT_AP, deviceSoftAP);
    }


    private WifiListFragment wifiListFragment;
    private WifiFacade wifiFacade;
    private SSID softApSSID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SEGAnalytics.screen("Device Setup: Select Network Screen");
        softApSSID = getIntent().getParcelableExtra(EXTRA_SOFT_AP);
        setContentView(R.layout.activity_select_network);

        wifiFacade = WifiFacade.get(this);
        wifiListFragment = Ui.findFrag(this, R.id.wifi_list_fragment);
        Ui.findView(this, R.id.action_rescan).setOnClickListener(v -> {
            ParticleUi.showParticleButtonProgress(SelectNetworkActivity.this, R.id.action_rescan, true);
            wifiListFragment.scanAsync();
        });
    }

    public void onManualNetworkEntryClicked(View view) {
        startActivity(ManualNetworkEntryActivity.buildIntent(this, softApSSID));
        finish();
    }

    @Override
    public void onNetworkSelected(ScanAPCommandResult selectedNetwork) {
        if (WifiSecurity.isEnterpriseNetwork(selectedNetwork.scan.wifiSecurityType)) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.enterprise_networks_not_supported))
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }
        wifiListFragment.stopAggroLoading();

        if (selectedNetwork.isSecured()) {
            SEGAnalytics.track("Device Setup: Selected secured network");
            startActivity(PasswordEntryActivity.buildIntent(this, softApSSID, selectedNetwork.scan));
        } else {
            SEGAnalytics.track("Device Setup: Selected open network");
            SSID softApSSID = wifiFacade.getCurrentlyConnectedSSID();
            startActivity(ConnectingActivity.buildIntent(this, softApSSID, selectedNetwork.scan));
        }
        finish();
    }

    @Override
    public Loader<Set<ScanAPCommandResult>> createLoader(int id, Bundle args) {
        // FIXME: make the address below use resources instead of hardcoding
        CommandClient client = CommandClient.newClientUsingDefaultsForDevices(this, softApSSID);
        return new ScanApCommandLoader(getApplicationContext(), client);
    }

    @Override
    public void onLoadFinished() {
        ParticleUi.showParticleButtonProgress(this, R.id.action_rescan, false);
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
