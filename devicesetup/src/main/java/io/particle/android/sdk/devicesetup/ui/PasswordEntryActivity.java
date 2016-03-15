package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.google.gson.Gson;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WiFi;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;


// FIXME: password validation -- check for correct length based on security type?
// at least check for minimum.
public class PasswordEntryActivity extends BaseActivity {

    public static final String EXTRA_NETWORK_TO_CONFIGURE = "EXTRA_NETWORK_TO_CONFIGURE";

    public static Intent buildIntent(Context ctx, ScanApCommand.Scan networkToConnectTo) {
        return new Intent(ctx, PasswordEntryActivity.class)
                .putExtra(EXTRA_NETWORK_TO_CONFIGURE, gson.toJson(networkToConnectTo));
    }


    private static final TLog log = TLog.get(PasswordEntryActivity.class);
    private static final Gson gson = new Gson();

    private CheckBox showPwdBox;
    private EditText passwordBox;
    private ScanApCommand.Scan networkToConnectTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_entry);

        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);

        String asJson = getIntent().getStringExtra(EXTRA_NETWORK_TO_CONFIGURE);
        networkToConnectTo = gson.fromJson(asJson, ScanApCommand.Scan.class);

        passwordBox = Ui.findView(this, R.id.password);
        passwordBox.requestFocus();

        showPwdBox = Ui.findView(this, R.id.show_password);

        initViews();
    }

    private void initViews() {
        Ui.setText(this, R.id.ssid, networkToConnectTo.ssid);
        Ui.setText(this, R.id.security_msg, getSecurityTypeMsg());

        // set up onClick (et al) listeners
        showPwdBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                togglePasswordVisibility(isChecked);
            }
        });
        // set up initial visibility state
        togglePasswordVisibility(showPwdBox.isChecked());
    }

    private void togglePasswordVisibility(boolean showPassword) {
        int inputType;
        if (showPassword) {
            inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        } else {
            inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        }
        passwordBox.setInputType(inputType);
    }

    private String getSecurityTypeMsg() {
        WifiSecurity securityType = WifiSecurity.fromInteger(networkToConnectTo.wifiSecurityType);
        // FIXME: turn into string resources
        switch (securityType) {
            case WEP_SHARED:
            case WEP_PSK:
                return getString(R.string.secured_with_wep);
            case WPA_AES_PSK:
            case WPA_TKIP_PSK:
            case WPA_MIXED_PSK:
                return getString(R.string.secured_with_wpa);
            case WPA2_AES_PSK:
            case WPA2_MIXED_PSK:
            case WPA2_TKIP_PSK:
                return getString(R.string.secured_with_wpa2);
        }

        log.e("No security string found for " + securityType + "!");
        return "";
    }

    public void onCancelClicked(View view) {
        startActivity(new Intent(this, SelectNetworkActivity.class));
        finish();
    }

    public void onConnectClicked(View view) {
        String secret = passwordBox.getText().toString();
        startActivity(ConnectingActivity.buildIntent(this,
                WiFi.getCurrentlyConnectedSSID(this),
                networkToConnectTo,
                secret));
        finish();
    }
}
