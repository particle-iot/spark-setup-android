package io.particle.android.sdk.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build.VERSION_CODES
import android.support.annotation.RequiresApi
import io.particle.android.sdk.utils.Funcy.Predicate
import io.particle.android.sdk.utils.Py.truthy
import java.util.*
import javax.annotation.ParametersAreNonnullByDefault


@ParametersAreNonnullByDefault
class WifiFacade private constructor(private val wifiManager: WifiManager, private val connectivityManager: ConnectivityManager) {

    val currentlyConnectedSSID: SSID?
        get() {
            val connectionInfo = connectionInfo
            return if (connectionInfo == null) {
                log.w("getCurrentlyConnectedSSID(): WifiManager.getConnectionInfo() returned null")
                null
            } else {
                val ssid = SSID.from(connectionInfo)
                log.d("Currently connected to: " + ssid +
                        ", supplicant state: " + connectionInfo.supplicantState)
                ssid
            }
        }

    val connectionInfo: WifiInfo?
        get() = wifiManager.connectionInfo

    // per the WifiManager docs, this seems like it should never return null, but I've
    // gotten null back before, possibly on an older API level.
    val scanResults: List<ScanResult>
        get() {
            val results = wifiManager.scanResults
            return results ?: emptyList()
        }

    var isWifiEnabled: Boolean = wifiManager.isWifiEnabled

    private val configuredNetworks: List<WifiConfiguration>
        get() {
            val configuredNetworks = wifiManager.configuredNetworks
            return configuredNetworks ?: emptyList()
        }

    fun getIdForConfiguredNetwork(ssid: SSID): Int {
        val configuredNetwork = Funcy.findFirstMatch(
                configuredNetworks
        ) { wifiConfiguration -> SSID.from(wifiConfiguration) == ssid }
        return if (configuredNetwork == null) {
            log.d("No network found (returning -1) for SSID: $ssid")
            -1
        } else {
            configuredNetwork.networkId
        }
    }

    fun reenableNetwork(ssid: SSID): Boolean {
        val networkId = getIdForConfiguredNetwork(ssid)
        return if (networkId == -1) {
            log.w("reenableNetwork(): no network found for SSID?? $ssid")
            false
        } else {
            log.d("Reenabling network configuration for:$ssid")
            wifiManager.enableNetwork(networkId, false)
        }
    }

    fun removeNetwork(ssid: SSID): Boolean {
        val networkId = getIdForConfiguredNetwork(ssid)
        return if (networkId == -1) {
            log.w("No network found for SSID $ssid")
            false
        } else {
            log.d("Removing network configuration for:$ssid")
            removeNetwork(networkId)
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    @Throws(NullPointerException::class)
    fun getNetworkForSSID(ssid: SSID?): Network? {
        // Android doesn't have any means of directly asking
        // "I want the Network obj for the Wi-Fi network with SSID <foo>".
        // Instead, you have to infer it based on a field.  Let's hope that
        // the behavior of "NetworkInfo.getExtraInfo()" doesn't ever change...
        return Funcy.findFirstMatch(
                Arrays.asList(*connectivityManager.allNetworks)
        ) { network ->
            val networkInfo = connectivityManager.getNetworkInfo(network)
            Py.truthy(networkInfo.extraInfo) && SSID.from(networkInfo.extraInfo) == ssid
        }
    }

    fun addNetwork(config: WifiConfiguration): Int {
        log.d("addNetwork with SSID " + config.SSID + ": " + config)
        return wifiManager.addNetwork(config)
    }

    fun disconnect(): Boolean {
        log.d("disconnect()")
        return wifiManager.disconnect()
    }

    fun enableNetwork(networkId: Int, disableOthers: Boolean): Boolean {
        log.d("enableNetwork for networkID $networkId")
        return wifiManager.enableNetwork(networkId, disableOthers)
    }

    fun getWifiConfiguration(ssid: SSID): WifiConfiguration? {
        val wifiConfigurations = configuredNetworks
        for (configuration in wifiConfigurations) {
            log.d("Found configured wifi: " + configuration.SSID)
            if (configuration.SSID == ssid.inQuotes()) {
                return configuration
            }
        }
        return null
    }

    fun reassociate() {
        log.d("reassociate")
        wifiManager.reassociate()
    }

    fun reconnect(): Boolean {
        log.d("reconnect")
        return wifiManager.reconnect()
    }

    fun removeNetwork(networkId: Int): Boolean {
        log.d("Removing network configuration for networkId: $networkId")
        return wifiManager.removeNetwork(networkId)
    }

    fun setWifiEnabled(enabled: Boolean): Boolean {
        log.d("setWifiEnabled: $enabled")
        return wifiManager.setWifiEnabled(enabled)
    }

    fun startScan(): Boolean {
        log.d("startScan()")
        return wifiManager.startScan()
    }

    companion object {
        private val log = TLog.get(WifiFacade::class.java)

        var is24Ghz = Predicate<ScanResult> { scanResult ->
            // this approach lifted from the ScanResult source
            scanResult.frequency in 2301..2499
        }

        // "truthy": see Py.truthy() javadoc
        // tl;dr:  not null or "" (empty string)
        var isWifiNameTruthy = Predicate<ScanResult> { scanResult -> truthy(scanResult.SSID) }

        operator fun get(ctx: Context): WifiFacade {
            val appCtx = ctx.applicationContext
            return WifiFacade(
                    appCtx.getSystemService(Context.WIFI_SERVICE) as WifiManager,
                    appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            )
        }
    }
}
