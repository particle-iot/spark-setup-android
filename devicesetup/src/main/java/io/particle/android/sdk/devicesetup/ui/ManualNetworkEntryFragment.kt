package io.particle.android.sdk.devicesetup.ui

import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox

import javax.inject.Inject

import androidx.navigation.Navigation
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory
import io.particle.android.sdk.devicesetup.commands.ScanApCommand
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity
import io.particle.android.sdk.devicesetup.loaders.ScanApCommandLoader
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.WifiFacade
import io.particle.android.sdk.utils.ui.ParticleUi
import io.particle.android.sdk.utils.ui.Ui
import kotlinx.android.synthetic.main.activity_manual_network_entry.view.*

class ManualNetworkEntryFragment : BaseFragment(), LoaderManager.LoaderCallbacks<Set<ScanAPCommandResult>> {

    @Inject
    lateinit var wifiFacade: WifiFacade
    @Inject
    lateinit var commandClientFactory: CommandClientFactory
    private var softApSSID: SSID? = null
    private var wifiSecurityType: Int? = WifiSecurity.WPA2_AES_PSK.asInt()

    fun onSecureCheckedChange(isChecked: Boolean) {
        wifiSecurityType = if (isChecked) {
            SEGAnalytics.track("Device Setup: Selected secured network")
            WifiSecurity.WPA2_AES_PSK.asInt()
        } else {
            SEGAnalytics.track("Device Setup: Selected open network")
            WifiSecurity.OPEN.asInt()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        SEGAnalytics.screen("Device Setup: Manual network entry screen")
        softApSSID = arguments!!.getParcelable(EXTRA_SOFT_AP)

        val view = inflater.inflate(R.layout.activity_manual_network_entry, container, true)
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(activity)

        view.network_requires_password.setOnCheckedChangeListener { _, isChecked ->
            onSecureCheckedChange(isChecked)
        }
        return view
    }

    fun onConnectClicked(view: View) {
        val ssid = Ui.getText(this, R.id.network_name, true)
        val scan = ScanApCommand.Scan(ssid, wifiSecurityType, 0)

        val requiresPassword = Ui.findView<CheckBox>(this, R.id.network_requires_password)
        if (requiresPassword.isChecked) {
            val bundle = Bundle()
            bundle.putParcelable(PasswordEntryFragment.EXTRA_SOFT_AP_SSID, softApSSID)
            bundle.putString(PasswordEntryFragment.EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                    .applicationComponent.gson.toJson(scan))
            Navigation.findNavController(view).navigate(R.id.action_manualNetworkEntryFragment_to_passwordEntryFragment, bundle)
        } else {
            val bundle = Bundle()
            bundle.putParcelable(ConnectingFragment.EXTRA_SOFT_AP_SSID, softApSSID)
            bundle.putString(ConnectingFragment.EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                    .applicationComponent.gson.toJson(scan))
            Navigation.findNavController(view).navigate(R.id.action_manualNetworkEntryFragment_to_connectingFragment, bundle)
        }
    }

    fun onCancelClicked(view: View) {
        //finish();
    }

    // FIXME: loader not currently used, see note in onLoadFinished()
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Set<ScanAPCommandResult>> {
        return ScanApCommandLoader(context!!, commandClientFactory.newClientUsingDefaultsForDevices(wifiFacade, softApSSID))
    }

    override fun onLoadFinished(loader: Loader<Set<ScanAPCommandResult>>, data: Set<ScanAPCommandResult>) {
        // FIXME: perform process described here?:
        // https://github.com/spark/mobile-sdk-ios/issues/56
    }

    override fun onLoaderReset(loader: Loader<Set<ScanAPCommandResult>>) {
        // no-op
    }

    companion object {
        const val EXTRA_SOFT_AP = "EXTRA_SOFT_AP"
    }
}
