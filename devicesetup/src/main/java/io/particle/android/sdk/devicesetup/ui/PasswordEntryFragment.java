package io.particle.android.sdk.devicesetup.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.gson.Gson;

import javax.inject.Inject;

import androidx.navigation.Navigation;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseFragment;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;

// FIXME: password validation -- check for correct length based on security type?
// at least check for minimum.
public class PasswordEntryFragment extends BaseFragment {
    public static final String
            EXTRA_NETWORK_TO_CONFIGURE = "EXTRA_NETWORK_TO_CONFIGURE",
            EXTRA_SOFT_AP_SSID = "EXTRA_SOFT_AP_SSID";


    private static final TLog log = TLog.get(PasswordEntryFragment.class);

    @BindView(R2.id.show_password) protected CheckBox showPwdBox;
    @BindView(R2.id.password) protected EditText passwordBox;
    private ScanApCommand.Scan networkToConnectTo;
    private SSID softApSSID;
    @Inject protected Gson gson;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_password_entry, container, false);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        ButterKnife.bind(this, view);
        SEGAnalytics.screen("Device Setup: Password Entry Screen");
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view);

        networkToConnectTo = gson.fromJson(
                getArguments().getString(EXTRA_NETWORK_TO_CONFIGURE),
                ScanApCommand.Scan.class);
        softApSSID = getArguments().getParcelable(EXTRA_SOFT_AP_SSID);
        passwordBox.requestFocus();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        initViews();
    }

    private void initViews() {
        Ui.setText(this, R.id.ssid, networkToConnectTo.ssid);
        Ui.setText(this, R.id.security_msg, getSecurityTypeMsg());

        // set up onClick (et al) listeners
        showPwdBox.setOnCheckedChangeListener((buttonView, isChecked) -> togglePasswordVisibility(isChecked));
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

    @OnClick(R2.id.action_cancel)
    public void onCancelClicked(View view) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SelectNetworkFragment.EXTRA_SOFT_AP, softApSSID);
        Navigation.findNavController(view).navigate(R.id.action_passwordEntryFragment_to_selectNetworkFragment, bundle);
    }

    @OnClick(R2.id.action_connect)
    public void onConnectClicked(View view) {
        String secret = passwordBox.getText().toString().trim();

        Bundle bundle = new Bundle();
        bundle.putParcelable(ConnectingFragment.EXTRA_SOFT_AP_SSID, softApSSID);
        bundle.putString(ConnectingFragment.EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                .getApplicationComponent().getGson().toJson(networkToConnectTo));
        bundle.putString(ConnectingFragment.EXTRA_NETWORK_SECRET, secret);
        Navigation.findNavController(view).navigate(R.id.action_passwordEntryFragment_to_connectingFragment, bundle);
    }
}
