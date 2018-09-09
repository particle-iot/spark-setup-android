package io.particle.android.sdk.devicesetup.ui

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.squareup.phrase.Phrase
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.devicesetup.ApConnector
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory
import io.particle.android.sdk.devicesetup.loaders.WifiScanResultLoader
import io.particle.android.sdk.devicesetup.model.ScanResultNetwork
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepException
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.*
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.ui.Ui
import io.particle.android.sdk.utils.ui.WebViewActivity
import kotlinx.android.synthetic.main.activity_discover_device.*
import kotlinx.android.synthetic.main.activity_discover_device.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import javax.inject.Inject

// FIXME: this activity is *far* too complicated.  Split it out into smaller components.
class DiscoverDeviceFragment : RequiresWifiScansFragment(), WifiListFragment.Client<ScanResultNetwork>, ApConnector.Client {
    override val listEmptyText: String
        get() = Phrase.from(activity!!, R.string.empty_soft_ap_list_text)
                .put("device_name", getString(R.string.device_name))
                .format().toString()
    override val aggroLoadingTimeMillis: Int
        get() = 5000

    @Inject
    lateinit var wifiFacade: WifiFacade
    @Inject
    lateinit var sparkCloud: ParticleCloud
    @Inject
    lateinit var discoverProcessWorker: DiscoverProcessWorker
    @Inject
    lateinit var softAPConfigRemover: SoftAPConfigRemover
    @Inject
    lateinit var commandClientFactory: CommandClientFactory

    private var wifiListFragment: WifiListFragment<*>? = null
    private var connectToApSpinnerDialog: ProgressDialog? = null

    private var connectToApJob: Job? = null
    private var resumed = false

    private var discoverProcessAttempts = 0

    private var selectedSoftApSSID: SSID? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.activity_discover_device, container, false)

        SEGAnalytics.screen("Device Setup: Device discovery screen")

        softAPConfigRemover.removeAllSoftApConfigs()
        softAPConfigRemover.reenableWifiNetworks()

        DeviceSetupState.previouslyConnectedWifiNetwork = wifiFacade.currentlyConnectedSSID

        wifiListFragment = Ui.findFrag(this, R.id.wifi_list_fragment)
        ConnectToApFragment.ensureAttached(this)
        resetWorker()

        Ui.setText(view, R.id.wifi_list_header,
                Phrase.from(activity!!, R.string.wifi_list_header_text)
                        .put("device_name", getString(R.string.device_name))
                        .format()
        )

        Ui.setText(view, R.id.msg_device_not_listed,
                Phrase.from(activity!!, R.string.msg_device_not_listed)
                        .put("device_name", getString(R.string.device_name))
                        .put("setup_button_identifier", getString(R.string.mode_button_name))
                        .put("indicator_light", getString(R.string.indicator_light))
                        .put("indicator_light_setup_color_name", getString(R.string.listen_mode_led_color_name))
                        .format()
        )

        Ui.setTextFromHtml(view, R.id.action_troubleshooting, R.string.troubleshooting)

        if (!truthy(sparkCloud.loggedInUsername)) {
            view.logged_in_as.visibility = View.GONE
        } else {
            Ui.setText(view, R.id.logged_in_as,
                    Phrase.from(activity!!, R.string.you_are_logged_in_as)
                            .put("username", sparkCloud.loggedInUsername!!)
                            .format()
            )
        }

        view.action_log_out.visibility = if (BaseFragment.setupOnly) View.GONE else View.VISIBLE
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_troubleshooting.setOnClickListener {
            val uri = Uri.parse(it.context.getString(R.string.troubleshooting_uri))
            startActivity(WebViewActivity.buildIntent(it.context, uri))
        }

        action_log_out.setOnClickListener {
            sparkCloud.logOut()
            log.i("logged out, username is: " + sparkCloud.loggedInUsername!!)
            Navigation.findNavController(view).navigate(R.id.action_discoverDeviceFragment_to_loginFragment)
        }

        action_cancel.setOnClickListener {
            Navigation.findNavController(view).navigateUp()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!wifiFacade.isWifiEnabled) {
            onWifiDisabled()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canGetLocation()) {
            onLocationDisabled()
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
    }

    override fun onPause() {
        super.onPause()
        resumed = false
    }

    private fun resetWorker() {
        discoverProcessWorker.withClient(commandClientFactory.newClientUsingDefaultsForDevices(wifiFacade, selectedSoftApSSID))
    }

    private fun onLocationDisabled() {
        log.d("Location disabled; prompting user")
        AlertDialog.Builder(context!!).setTitle(R.string.location_required)
                .setMessage(R.string.location_required_message)
                .setPositiveButton(R.string.enable_location) { dialog, _ ->
                    dialog.dismiss()
                    log.i("Sending user to enabling Location services.")
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton(R.string.exit_setup) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    private fun canGetLocation(): Boolean {
        var gpsEnabled = false
        var networkEnabled = false
        val lm = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        try {
            if (lm != null) {
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (ignored: Exception) {
        }

        return gpsEnabled || networkEnabled
    }

    private fun onWifiDisabled() {
        log.d("Wi-Fi disabled; prompting user")
        AlertDialog.Builder(context!!)
                .setTitle(R.string.wifi_required)
                .setPositiveButton(R.string.enable_wifi) { dialog, _ ->
                    dialog.dismiss()
                    log.i("Enabling Wi-Fi at the user's request.")
                    wifiFacade.isWifiEnabled = true
                    wifiListFragment!!.scanAsync()
                }
                .setNegativeButton(R.string.exit_setup) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    override fun onNetworkSelected(selectedNetwork: ScanResultNetwork?) {
        val wifiConfig = ApConnector.buildUnsecuredConfig(selectedNetwork?.ssid)
        selectedSoftApSSID = selectedNetwork?.ssid
        resetWorker()
        connectToSoftAp(wifiConfig)
    }

    private fun connectToSoftAp(config: WifiConfiguration) {
        discoverProcessAttempts++
        softAPConfigRemover.onSoftApConfigured(SSID.from(config.SSID))
        ConnectToApFragment[this]?.connectToAP(config)
        showProgressDialog()
    }

    override fun createLoader(id: Int, args: Bundle?): Loader<Set<ScanResultNetwork>> {
        return WifiScanResultLoader(context!!, wifiFacade)
    }

    override fun onLoadFinished() {
        // no-op
    }

    override fun onApConnectionSuccessful(config: WifiConfiguration) {
        startConnectWorker()
    }

    override fun onApConnectionFailed(config: WifiConfiguration) {
        hideProgressDialog()

        if (!canStartProcessAgain()) {
            onMaxAttemptsReached()
        } else {
            connectToSoftAp(config)
        }
    }

    private fun showProgressDialog() {
        wifiListFragment!!.stopAggroLoading()

        val msg = Phrase.from(activity!!, R.string.connecting_to_soft_ap)
                .put("device_name", getString(R.string.device_name))
                .format().toString()

        connectToApSpinnerDialog = ProgressDialog(context)
        connectToApSpinnerDialog!!.setMessage(msg)
        connectToApSpinnerDialog!!.setCancelable(false)
        connectToApSpinnerDialog!!.isIndeterminate = true
        connectToApSpinnerDialog!!.show()
    }

    private fun hideProgressDialog() {
        wifiListFragment!!.startAggroLoading()
        if (connectToApSpinnerDialog != null) {
            if (!activity!!.isFinishing) {
                connectToApSpinnerDialog!!.dismiss()
            }
            connectToApSpinnerDialog = null
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun startConnectWorker() {
        // first, make sure we haven't actually been called twice...
        if (connectToApJob != null) {
            log.d("Already running connect worker $connectToApJob, refusing to start another")
            return
        }

        wifiListFragment!!.stopAggroLoading()
        // FIXME: verify first that we're still connected to the intended network
        if (!canStartProcessAgain()) {
            hideProgressDialog()
            onMaxAttemptsReached()
            return
        }

        discoverProcessAttempts++

        // This just has withContext() return null on success, or if an
        // exception was thrown, it passes that along instead to indicate failure.
        connectToApJob = launch(UI) {
            val error = withContext(CommonPool) {
                try {
                    // including this sleep because without it,
                    // we seem to attempt a socket connection too early,
                    // and it makes the process time out(!)
                    log.d("Waiting a couple seconds before trying the socket connection...")
                    EZ.threadSleep(2000)
                    discoverProcessWorker.doTheThing()
                    null
                } catch (e: SetupStepException) {
                    log.d("Setup exception thrown: ", e)
                    e
                }
            }

            if (error == null || BaseFragment.setupOnly && error is DiscoverDeviceFragment.DeviceAlreadyClaimed) {
                // no exceptions thrown, huzzah
                hideProgressDialog()

                val bundle = Bundle()
                bundle.putParcelable(SelectNetworkFragment.EXTRA_SOFT_AP, selectedSoftApSSID)
                Navigation.findNavController(view!!).navigate(R.id.action_discoverDeviceFragment_to_selectNetworkFragment, bundle)
            } else if (error is DiscoverDeviceFragment.DeviceAlreadyClaimed) {
                hideProgressDialog()
                onDeviceClaimedByOtherUser()
            } else {
                // nope, do it all over again.
                // FIXME: this might be a good time to display some feedback...
                startConnectWorker()
            }
        }
    }

    private fun canStartProcessAgain(): Boolean {
        return discoverProcessAttempts < MAX_NUM_DISCOVER_PROCESS_ATTEMPTS
    }

    private fun onMaxAttemptsReached() {
        if (!isResumed) {
            return
        }

        val errorMsg = Phrase.from(activity!!, R.string.unable_to_connect_to_soft_ap)
                .put("device_name", getString(R.string.device_name))
                .format().toString()

        AlertDialog.Builder(context!!)
                .setTitle(R.string.error)
                .setMessage(errorMsg)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    Navigation.findNavController(view!!).navigate(R.id.action_discoverDeviceFragment_to_getReadyFragment)
                }
                .show()
    }

    private fun onDeviceClaimedByOtherUser() {
        val dialogMsg = getString(R.string.dialog_title_owned_by_another_user,
                getString(R.string.device_name), sparkCloud.loggedInUsername)

        AlertDialog.Builder(context!!)
                .setTitle(getString(R.string.change_owner_question))
                .setMessage(dialogMsg)
                .setPositiveButton(getString(R.string.change_owner)
                ) { dialog, _ ->
                    dialog.dismiss()
                    log.i("Changing owner to " + sparkCloud.loggedInUsername!!)
                    // FIXME: state mutation from another class.  Not pretty.
                    // Fix this by breaking DiscoverProcessWorker down into Steps
                    resetWorker()
                    discoverProcessWorker.needToClaimDevice = true
                    discoverProcessWorker.gotOwnershipInfo = true
                    discoverProcessWorker.isDetectedDeviceClaimed = false

                    showProgressDialog()
                    startConnectWorker()
                }
                .setNegativeButton(R.string.cancel
                ) { dialog, _ ->
                    dialog.dismiss()
                    Navigation.findNavController(view!!).navigate(R.id.action_discoverDeviceFragment_to_getReadyFragment)
                }
                .show()
    }

    // FIXME: remove this if we break down discover process worker into Steps
    // no data to pass along with this at the moment, I just want to specify
    // that this isn't an error which should necessarily count against retries.
    internal class DeviceAlreadyClaimed(msg: String) : SetupStepException(msg)

    companion object {
        // see ApConnector for the timeout value used for connecting to the soft AP
        private const val MAX_NUM_DISCOVER_PROCESS_ATTEMPTS = 5

        private val log = TLog.get(DiscoverDeviceFragment::class.java)
    }

}
