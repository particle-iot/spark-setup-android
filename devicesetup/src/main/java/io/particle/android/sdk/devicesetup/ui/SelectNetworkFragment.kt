package io.particle.android.sdk.devicesetup.ui

import android.os.Bundle
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import javax.inject.Inject

import androidx.navigation.Navigation
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity
import io.particle.android.sdk.devicesetup.loaders.ScanApCommandLoader
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.WifiFacade
import io.particle.android.sdk.utils.ui.ParticleUi
import io.particle.android.sdk.utils.ui.Ui
import kotlinx.android.synthetic.main.activity_select_network.view.*

class SelectNetworkFragment : RequiresWifiScansFragment(), WifiListFragment.Client<ScanAPCommandResult> {
    override val listEmptyText: String
        get() = getString(R.string.no_wifi_networks_found)
    override val aggroLoadingTimeMillis: Int
        get() = 10000

    private var wifiListFragment: WifiListFragment<*>? = null
    @Inject
    lateinit var wifiFacade: WifiFacade
    @Inject
    lateinit var commandClientFactory: CommandClientFactory
    private var softApSSID: SSID? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        SEGAnalytics.screen("Device Setup: Select Network Screen")

        softApSSID = arguments!!.getParcelable(EXTRA_SOFT_AP)

        val view = inflater.inflate(R.layout.activity_select_network, container, false)

        wifiListFragment = Ui.findFrag(this, R.id.wifi_list_fragment)

        view.action_rescan.setOnClickListener {
            ParticleUi.showParticleButtonProgress(view, R.id.action_rescan, true)
            wifiListFragment!!.scanAsync()
        }

        view.action_manual_network_entry.setOnClickListener {
            onManualNetworkEntryClicked(it)
        }

        return view
    }

    private fun onManualNetworkEntryClicked(view: View) {
        val bundle = Bundle()
        bundle.putParcelable(ManualNetworkEntryFragment.EXTRA_SOFT_AP, softApSSID)
        Navigation.findNavController(getView()!!).navigate(R.id.action_selectNetworkFragment_to_manualNetworkEntryFragment, bundle)
    }

    override fun onNetworkSelected(selectedNetwork: ScanAPCommandResult?) {
        if (WifiSecurity.isEnterpriseNetwork(selectedNetwork?.scan?.wifiSecurityType!!)) {
            AlertDialog.Builder(context!!)
                    .setMessage(getString(R.string.enterprise_networks_not_supported))
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
            return
        }
        wifiListFragment!!.stopAggroLoading()

        if (selectedNetwork.isSecured) {
            SEGAnalytics.track("Device Setup: Selected secured network")

            val bundle = Bundle()
            bundle.putParcelable(PasswordEntryFragment.EXTRA_SOFT_AP_SSID, softApSSID)
            bundle.putString(PasswordEntryFragment.EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                    .applicationComponent.gson.toJson(selectedNetwork.scan))
            Navigation.findNavController(view!!).navigate(R.id.action_selectNetworkFragment_to_passwordEntryFragment, bundle)
        } else {
            SEGAnalytics.track("Device Setup: Selected open network")
            val softApSSID = wifiFacade.currentlyConnectedSSID

            val bundle = Bundle()
            bundle.putParcelable(ConnectingFragment.EXTRA_SOFT_AP_SSID, softApSSID)
            bundle.putString(ConnectingFragment.EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                    .applicationComponent.gson.toJson(selectedNetwork.scan))
            Navigation.findNavController(view!!).navigate(R.id.action_selectNetworkFragment_to_connectingFragment, bundle)
        }
    }

    override fun createLoader(id: Int, args: Bundle?): Loader<Set<ScanAPCommandResult>> {
        // FIXME: make the address below use resources instead of hardcoding
        val client = commandClientFactory.newClientUsingDefaultsForDevices(wifiFacade, softApSSID)
        return ScanApCommandLoader(context!!, client)
    }

    override fun onLoadFinished() {
        ParticleUi.showParticleButtonProgress(view, R.id.action_rescan, false)
    }

    companion object {
        const val EXTRA_SOFT_AP = "deviceSoftAP"
    }

}
