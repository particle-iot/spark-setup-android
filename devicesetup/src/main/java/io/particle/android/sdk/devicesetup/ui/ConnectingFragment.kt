package io.particle.android.sdk.devicesetup.ui

import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.squareup.phrase.Phrase
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.devicesetup.ApConnector
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory
import io.particle.android.sdk.devicesetup.commands.ScanApCommand
import io.particle.android.sdk.devicesetup.setupsteps.SetupStep
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepApReconnector
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsFactory
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.*
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.ui.Ui
import kotlinx.android.synthetic.main.activity_connecting.*
import java.security.PublicKey
import javax.inject.Inject

class ConnectingFragment : BaseFragment() {

    // FIXME: all this state needs to be configured and encapsulated better
    private var connectingProcessWorkerTask: ConnectingProcessWorkerTask? = null
    @Inject
    lateinit var softAPConfigRemover: SoftAPConfigRemover
    @Inject
    lateinit var wifiFacade: WifiFacade
    @Inject
    lateinit var apConnector: ApConnector
    @Inject
    lateinit var commandClientFactory: CommandClientFactory
    @Inject
    lateinit var setupStepsFactory: SetupStepsFactory
    @Inject
    lateinit var sparkCloud: ParticleCloud
    @Inject
    lateinit var gson: Gson

    private var networkToConnectTo: ScanApCommand.Scan? = null
    private var networkSecretPlaintext: String? = null
    private var publicKey: PublicKey? = null
    private var deviceSoftApSsid: SSID? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.activity_connecting, container, false)

        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        SEGAnalytics.screen("Device Setup: Connecting progress screen")
        publicKey = DeviceSetupState.publicKey
        deviceSoftApSsid = arguments!!.getParcelable(EXTRA_SOFT_AP_SSID)

        val asJson = arguments!!.getString(EXTRA_NETWORK_TO_CONFIGURE)
        networkToConnectTo = gson.fromJson(asJson, ScanApCommand.Scan::class.java)
        networkSecretPlaintext = arguments!!.getString(EXTRA_NETWORK_SECRET)

        log.d("Connecting to " + networkToConnectTo + ", with networkSecretPlaintext of size: "
                + if (networkSecretPlaintext == null) 0 else networkSecretPlaintext!!.length)

        connectingProcessWorkerTask = ConnectingProcessWorkerTask(activity!!, buildSteps(), 15)
        connectingProcessWorkerTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Ui.setText(view, R.id.network_name, networkToConnectTo!!.ssid)
        Ui.setText(view, R.id.connecting_text,
                Phrase.from(activity!!, R.string.connecting_text)
                        .put("device_name", getString(R.string.device_name))
                        .format()
        )
        Ui.setText(view, R.id.network_name, networkToConnectTo!!.ssid)

        action_cancel.setOnClickListener {
            if (connectingProcessWorkerTask != null && !connectingProcessWorkerTask!!.isCancelled) {
                connectingProcessWorkerTask!!.cancel(false)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (connectingProcessWorkerTask != null && !connectingProcessWorkerTask!!.isCancelled) {
            connectingProcessWorkerTask!!.cancel(true)
            connectingProcessWorkerTask = null
        }
        apConnector.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        softAPConfigRemover.removeAllSoftApConfigs()
        softAPConfigRemover.reenableWifiNetworks()
    }

    private fun buildSteps(): List<SetupStep> {
        val commandClient = commandClientFactory.newClientUsingDefaultsForDevices(wifiFacade, deviceSoftApSsid)
        val reconnector = SetupStepApReconnector(wifiFacade, apConnector, Handler(), deviceSoftApSsid!!)

        val configureAPStep = setupStepsFactory.newConfigureApStep(commandClient,
                reconnector, networkToConnectTo!!, networkSecretPlaintext!!, publicKey!!)

        val connectDeviceToNetworkStep = setupStepsFactory
                .newConnectDeviceToNetworkStep(commandClient, reconnector)

        val waitForDisconnectionFromDeviceStep = setupStepsFactory
                .newWaitForDisconnectionFromDeviceStep(deviceSoftApSsid!!, wifiFacade)

        val ensureSoftApNotVisible = setupStepsFactory
                .newEnsureSoftApNotVisible(deviceSoftApSsid, wifiFacade)

        val waitForLocalCloudConnectivityStep = setupStepsFactory
                .newWaitForCloudConnectivityStep(context!!.applicationContext)

        val checkIfDeviceClaimedStep = setupStepsFactory
                .newCheckIfDeviceClaimedStep(sparkCloud, DeviceSetupState.deviceToBeSetUpId!!)

        val steps = list(
                configureAPStep,
                connectDeviceToNetworkStep,
                waitForDisconnectionFromDeviceStep,
                ensureSoftApNotVisible,
                waitForLocalCloudConnectivityStep)
        if (!BaseFragment.setupOnly) {
            steps.add(checkIfDeviceClaimedStep)
        }
        return steps
    }

    companion object {
        const val EXTRA_NETWORK_TO_CONFIGURE = "EXTRA_NETWORK_TO_CONFIGURE"
        const val EXTRA_NETWORK_SECRET = "EXTRA_NETWORK_SECRET"
        const val EXTRA_SOFT_AP_SSID = "EXTRA_SOFT_AP_SSID"

        private val log = TLog.get(ConnectingFragment::class.java)
    }
}
