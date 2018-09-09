package io.particle.android.sdk.devicesetup

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.MainThread
import io.particle.android.sdk.utils.*
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.Py.truthy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


@MainThread
class ApConnector(val ctx: Context, private val softAPConfigRemover: SoftAPConfigRemover, private val wifiFacade: WifiFacade) {

    private val client: DecoratedClient
    private val wifiLogger: SimpleReceiver
    private val mainThreadHandler: Handler
    private val setupRunnables = list<Runnable>()

    private var wifiStateChangeListener: SimpleReceiver? = null
    private var onTimeoutRunnable: Runnable? = null

    interface Client {

        fun onApConnectionSuccessful(config: WifiConfiguration)

        fun onApConnectionFailed(config: WifiConfiguration)

    }

    init {
        this.client = DecoratedClient()
        this.mainThreadHandler = Handler(Looper.getMainLooper())
        this.wifiLogger = SimpleReceiver.newReceiver(ctx.applicationContext,
                IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION),
                object : SimpleReceiver.LambdafiableBroadcastReceiver {
                    override fun onReceive(context: Context, intent: Intent) {
                        log.d("Received " + WifiManager.NETWORK_STATE_CHANGED_ACTION)
                        log.d("EXTRA_NETWORK_INFO: " + intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO))
                        // this will only be present if the new state is CONNECTED
                        val wifiInfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
                        log.d("WIFI_INFO: $wifiInfo")
                    }
                }
        )
    }

    /**
     * Connect this Android device to the specified AP.
     *
     * @param config the WifiConfiguration defining which AP to connect to
     * @return the SSID that was connected prior to calling this method.  Will be null if
     * there was no network connected, or if already connected to the target network.
     */
    fun connectToAP(config: WifiConfiguration, client: Client): SSID? {
        wifiLogger.register()
        this.client.decoratedClient = client

        // cancel any currently running timeout, etc
        clearState()

        val configSSID = SSID.from(config)
        val currentConnectionInfo = wifiFacade.connectionInfo
        // are we already connected to the right AP?  (this could happen on retries)
        if (isAlreadyConnectedToTargetNetwork(currentConnectionInfo, configSSID)) {
            // we're already connected to this AP, nothing to do.
            client.onApConnectionSuccessful(config)
            return null
        }

        scheduleTimeoutCheck(CONNECT_TO_DEVICE_TIMEOUT_MILLIS, config)
        wifiStateChangeListener = SimpleReceiver.newRegisteredReceiver(ctx.applicationContext,
                WIFI_STATE_CHANGE_FILTER,
                object : SimpleReceiver.LambdafiableBroadcastReceiver {
                    override fun onReceive(context: Context, intent: Intent) {
                        onWifiChangeBroadcastReceived(intent, config)
                    }
                })
        val useMoreComplexConnectionProcess = Build.VERSION.SDK_INT < 18


        // we don't need this for its atomicity, we just need it as a 'final' reference to an
        // integer which can be shared by a couple of the Runnables below
        val networkID = AtomicInteger(-1)

        // everything below is created in Runnables and scheduled on the runloop to avoid some
        // wonkiness I ran into when trying to do every one of these steps one right after
        // the other on the same thread.

        val alreadyConfiguredId = wifiFacade.getIdForConfiguredNetwork(configSSID)
        if (alreadyConfiguredId != -1 && !useMoreComplexConnectionProcess) {
            // For some unexplained (and probably sad-trombone-y) reason, if the AP specified was
            // already configured and had been connected to in the past, it will often get to
            // the "CONNECTING" event, but just before firing the "CONNECTED" event, the
            // WifiManager appears to change its mind and reconnects to whatever configured and
            // available AP it feels like.
            //
            // As a remedy, we pre-emptively remove that config.  *shakes fist toward Mountain View*

            setupRunnables.add(Runnable {
                if (wifiFacade.removeNetwork(alreadyConfiguredId)) {
                    log.d("Removed already-configured $configSSID network successfully")
                } else {
                    log.e("Somehow failed to remove the already-configured network!?")
                    // not calling this state an actual failure, since it might succeed anyhow,
                    // and if it doesn't, the worst case is a longer wait to find that out.
                }
            })
        }

        if (alreadyConfiguredId == -1 || !useMoreComplexConnectionProcess) {
            setupRunnables.add(Runnable {
                log.d("Adding network $configSSID")
                networkID.set(wifiFacade.addNetwork(config))

                if (networkID.get() == -1) {
                    val configuration = wifiFacade.getWifiConfiguration(configSSID)
                    if (configuration != null) {
                        networkID.set(configuration.networkId)
                    }
                }

                if (networkID.get() == -1) {
                    log.e("Adding network $configSSID failed.")
                    client.onApConnectionFailed(config)

                } else {
                    log.i("Added network with ID $networkID successfully")
                }
            })
        }

        if (useMoreComplexConnectionProcess) {
            setupRunnables.add(Runnable {
                log.d("Disconnecting from networks; reconnecting momentarily.")
                wifiFacade.disconnect()
            })
        }

        setupRunnables.add(Runnable {
            log.i("Enabling network " + configSSID + " with network ID " + networkID.get())
            wifiFacade.enableNetwork(networkID.get(), !useMoreComplexConnectionProcess)
        })

        if (useMoreComplexConnectionProcess) {
            setupRunnables.add(Runnable {
                log.d("Disconnecting from networks; reconnecting momentarily.")
                wifiFacade.reconnect()
            })
        }

        val currentlyConnectedSSID = wifiFacade.currentlyConnectedSSID
        softAPConfigRemover.onWifiNetworkDisabled(currentlyConnectedSSID!!)

        var timeout: Long = 0
        for (runnable in setupRunnables) {
            EZ.runOnMainThreadDelayed(timeout, runnable)
            timeout += 1500
        }

        return SSID.from(currentConnectionInfo)
    }

    fun stop() {
        client.decoratedClient = null
        clearState()
        wifiLogger.unregister()
    }

    private fun scheduleTimeoutCheck(timeoutInMillis: Long, config: WifiConfiguration) {
        onTimeoutRunnable = Runnable { client.onApConnectionFailed(config) }
        mainThreadHandler.postDelayed(onTimeoutRunnable, timeoutInMillis)
    }

    private fun clearState() {
        if (onTimeoutRunnable != null) {
            mainThreadHandler.removeCallbacks(onTimeoutRunnable)
            onTimeoutRunnable = null
        }

        if (wifiStateChangeListener != null) {
            ctx.applicationContext.unregisterReceiver(wifiStateChangeListener)
            wifiStateChangeListener = null
        }

        for (runnable in setupRunnables) {
            mainThreadHandler.removeCallbacks(runnable)
        }
        setupRunnables.clear()
    }

    private fun onWifiChangeBroadcastReceived(intent: Intent, config: WifiConfiguration) {
        // this will only be present if the new state is CONNECTED
        val wifiInfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
        if (wifiInfo == null || wifiInfo.ssid == null) {
            // no WifiInfo or SSID means we're not interested.
            return
        }
        val newlyConnectedSSID = SSID.from(wifiInfo)
        log.i("Connected to: $newlyConnectedSSID")
        if (newlyConnectedSSID == SSID.from(config)) {
            // FIXME: find a way to record success in memory in case this happens to happen
            // during a config change (etc)?
            client.onApConnectionSuccessful(config)
        }
    }


    // a Client decorator to ensure clearState() is called every time
    private inner class DecoratedClient : Client {

        internal var decoratedClient: Client? = null

        override fun onApConnectionSuccessful(config: WifiConfiguration) {
            clearState()
            if (decoratedClient != null) {
                decoratedClient!!.onApConnectionSuccessful(config)
            }
        }

        override fun onApConnectionFailed(config: WifiConfiguration) {
            clearState()
            if (decoratedClient != null) {
                decoratedClient!!.onApConnectionFailed(config)
            }
        }
    }

    companion object {

        fun buildUnsecuredConfig(ssid: SSID?): WifiConfiguration {
            val config = WifiConfiguration()
            config.SSID = ssid?.inQuotes()
            config.hiddenSSID = false
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            // have to set a very high number in order to ensure that Android doesn't
            // immediately drop this connection and reconnect to the a different AP
            config.priority = 999999
            return config
        }


        private val log = TLog.get(ApConnector::class.java)

        val CONNECT_TO_DEVICE_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20)

        private val WIFI_STATE_CHANGE_FILTER = IntentFilter(
                WifiManager.NETWORK_STATE_CHANGED_ACTION)


        private fun isAlreadyConnectedToTargetNetwork(currentConnectionInfo: WifiInfo?,
                                                      targetNetworkSsid: SSID): Boolean {
            return isCurrentlyConnectedToAWifiNetwork(currentConnectionInfo) && targetNetworkSsid == SSID.from(currentConnectionInfo)
        }

        private fun isCurrentlyConnectedToAWifiNetwork(currentConnectionInfo: WifiInfo?): Boolean {
            return (currentConnectionInfo != null
                    && truthy(currentConnectionInfo.ssid)
                    && currentConnectionInfo.networkId != -1
                    // yes, this happens.  Thanks, Android.
                    && "0x" != currentConnectionInfo.ssid)
        }
    }

}
