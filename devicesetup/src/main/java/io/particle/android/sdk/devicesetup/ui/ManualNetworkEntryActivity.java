package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.Set;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.devicesetup.loaders.ScanApCommandLoader;
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;


public class ManualNetworkEntryActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Set<ScanAPCommandResult>> {


    public static Intent buildIntent(Context ctx, SSID softApSSID) {
        return new Intent(ctx, ManualNetworkEntryActivity.class)
                .putExtra(EXTRA_SOFT_AP, softApSSID);
    }


    private static final String EXTRA_SOFT_AP = "EXTRA_SOFT_AP";


    private WifiFacade wifiFacade;
    private SSID softApSSID;

    private final CompoundButton.OnCheckedChangeListener secureCheckListener = (buttonView, isChecked) -> {
        if (isChecked) {
            SEGAnalytics.track("Device Setup: Selected secured network");
        } else {
            SEGAnalytics.track("Device Setup: Selected open network");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SEGAnalytics.screen("Device Setup: Manual network entry screen");
        softApSSID = getIntent().getParcelableExtra(EXTRA_SOFT_AP);
        wifiFacade = WifiFacade.get(this);

        setContentView(R.layout.activity_manual_network_entry);
        CheckBox secureCheckbox = Ui.findView(this, R.id.network_requires_password);
        secureCheckbox.setOnCheckedChangeListener(secureCheckListener);
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);
    }

    public void onConnectClicked(View view) {
        String ssid = Ui.getText(this, R.id.network_name, true);
        ScanApCommand.Scan scan = new ScanApCommand.Scan(ssid, WifiSecurity.WPA2_AES_PSK.asInt(), 0);

        CheckBox requiresPassword = Ui.findView(this, R.id.network_requires_password);
        if (requiresPassword.isChecked()) {
            startActivity(PasswordEntryActivity.buildIntent(this, softApSSID, scan));

        } else {
            startActivity(ConnectingActivity.buildIntent(this, softApSSID, scan));
        }
    }

    public void onCancelClicked(View view) {
        finish();
    }

    // FIXME: loader not currently used, see note in onLoadFinished()
    @Override
    public Loader<Set<ScanAPCommandResult>> onCreateLoader(int id, Bundle args) {
        return new ScanApCommandLoader(this,
                CommandClient.newClientUsingDefaultsForDevices(this, softApSSID));
    }

    @Override
    public void onLoadFinished(Loader<Set<ScanAPCommandResult>> loader, Set<ScanAPCommandResult> data) {
        // FIXME: perform process described here?:
        // https://github.com/spark/mobile-sdk-ios/issues/56
    }

    @Override
    public void onLoaderReset(Loader<Set<ScanAPCommandResult>> loader) {
        // no-op
    }
}
