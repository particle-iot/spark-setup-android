package io.particle.android.sdk.devicesetup.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog.Builder;
import android.view.View;

import java.util.Set;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.InterfaceBindingSocketFactory;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.devicesetup.loaders.ScanApCommandLoader;
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult;
import io.particle.android.sdk.utils.WiFi;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;


public class SelectNetworkActivity extends RequiresWifiScansActivity
        implements WifiListFragment.Client<ScanAPCommandResult> {


    private WifiListFragment wifiListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_network);

        wifiListFragment = Ui.findFrag(this, R.id.wifi_list_fragment);
        Ui.findView(this, R.id.action_rescan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParticleUi.showParticleButtonProgress(SelectNetworkActivity.this, R.id.action_rescan, true);
                wifiListFragment.scanAsync();
            }
        });
    }

    public void onManualNetworkEntryClicked(View view) {
        startActivity(new Intent(this, ManualNetworkEntryActivity.class));
        finish();
    }

    @Override
    public void onNetworkSelected(ScanAPCommandResult selectedNetwork) {
        if (WifiSecurity.isEnterpriseNetwork(selectedNetwork.scan.wifiSecurityType)) {
            new Builder(this)
                    .setMessage(getString(R.string.enterprise_networks_not_supported))
                    .setPositiveButton(R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            return;
        }

        wifiListFragment.stopAggroLoading();

        String softApSSID = WiFi.getCurrentlyConnectedSSID(this);
        if (selectedNetwork.isSecured()) {
            startActivity(PasswordEntryActivity.buildIntent(this, selectedNetwork.scan));
        } else {
            startActivity(ConnectingActivity.buildIntent(this, softApSSID, selectedNetwork.scan));
        }
        finish();
    }

    @Override
    public Loader<Set<ScanAPCommandResult>> createLoader(int id, Bundle args) {
        // FIXME: make the address below use resources instead of hardcoding
        CommandClient client = CommandClient.newClientUsingDefaultSocketAddress();
        return new ScanApCommandLoader(getApplicationContext(), client,
                new InterfaceBindingSocketFactory(this));
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
