package io.particle.android.sdk.devicesetup.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.google.gson.Gson
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.commands.ScanApCommand
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.ParticleUi
import io.particle.android.sdk.utils.ui.Ui
import kotlinx.android.synthetic.main.activity_password_entry.*
import kotlinx.android.synthetic.main.activity_password_entry.view.*
import javax.inject.Inject

// FIXME: password validation -- check for correct length based on security type?
// at least check for minimum.
class PasswordEntryFragment : BaseFragment() {

    private var networkToConnectTo: ScanApCommand.Scan? = null
    private var softApSSID: SSID? = null
    @Inject
    lateinit var gson: Gson

    private val securityTypeMsg: String
        get() {
            val securityType = WifiSecurity.fromInteger(networkToConnectTo!!.wifiSecurityType)
            when (securityType) {
                WifiSecurity.WEP_SHARED, WifiSecurity.WEP_PSK -> return getString(R.string.secured_with_wep)
                WifiSecurity.WPA_AES_PSK, WifiSecurity.WPA_TKIP_PSK, WifiSecurity.WPA_MIXED_PSK -> return getString(R.string.secured_with_wpa)
                WifiSecurity.WPA2_AES_PSK, WifiSecurity.WPA2_MIXED_PSK, WifiSecurity.WPA2_TKIP_PSK -> return getString(R.string.secured_with_wpa2)
                else -> {
                    log.e("No security string found for $securityType!")
                }
            }
            return ""
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.activity_password_entry, container, false)

        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        SEGAnalytics.screen("Device Setup: Password Entry Screen")
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view)

        networkToConnectTo = gson.fromJson(
                arguments!!.getString(EXTRA_NETWORK_TO_CONFIGURE),
                ScanApCommand.Scan::class.java)
        softApSSID = arguments!!.getParcelable(EXTRA_SOFT_AP_SSID)
        password.requestFocus()

        view.action_cancel.setOnClickListener {
            onCancelClicked(it)
        }

        view.action_connect.setOnClickListener {
            onConnectClicked(it)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        Ui.setText(this, R.id.ssid, networkToConnectTo!!.ssid)
        Ui.setText(this, R.id.security_msg, securityTypeMsg)

        // set up onClick (et al) listeners
        show_password.setOnCheckedChangeListener { _, isChecked -> togglePasswordVisibility(isChecked) }
        // set up initial visibility state
        togglePasswordVisibility(show_password.isChecked)
    }

    private fun togglePasswordVisibility(showPassword: Boolean) {
        password.inputType = if (showPassword) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    fun onCancelClicked(view: View) {
        val bundle = Bundle()
        bundle.putParcelable(SelectNetworkFragment.EXTRA_SOFT_AP, softApSSID)
        Navigation.findNavController(view).navigate(R.id.action_passwordEntryFragment_to_selectNetworkFragment, bundle)
    }

    fun onConnectClicked(view: View) {
        val secret = password.text.toString().trim { it <= ' ' }

        val bundle = Bundle()
        bundle.putParcelable(ConnectingFragment.EXTRA_SOFT_AP_SSID, softApSSID)
        bundle.putString(ConnectingFragment.EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                .applicationComponent.gson.toJson(networkToConnectTo))
        bundle.putString(ConnectingFragment.EXTRA_NETWORK_SECRET, secret)
        Navigation.findNavController(view).navigate(R.id.action_passwordEntryFragment_to_connectingFragment, bundle)
    }

    companion object {
        const val EXTRA_NETWORK_TO_CONFIGURE = "EXTRA_NETWORK_TO_CONFIGURE"
        const val EXTRA_SOFT_AP_SSID = "EXTRA_SOFT_AP_SSID"

        private val log = TLog.get(PasswordEntryFragment::class.java)
    }
}
