package io.particle.android.sdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

import io.particle.android.sdk.utils.Funcy.transformSet
import io.particle.android.sdk.utils.Py.set


class SoftAPConfigRemover(context: Context, private val wifiFacade: WifiFacade) {


    private val prefs: SharedPreferences

    init {
        val ctx = context.applicationContext
        prefs = ctx.getSharedPreferences(PREFS_SOFT_AP_NETWORK_REMOVER, Context.MODE_PRIVATE)
    }

    fun onSoftApConfigured(newSsid: SSID) {
        // make a defensive copy of what we get back
        val ssids = set(loadSSIDsWithKey(KEY_SOFT_AP_SSIDS))
        ssids.add(newSsid)
        saveWithKey(KEY_SOFT_AP_SSIDS, ssids)
    }

    fun removeAllSoftApConfigs() {
        for (ssid in loadSSIDsWithKey(KEY_SOFT_AP_SSIDS)) {
            wifiFacade.removeNetwork(ssid)
        }
        saveWithKey(KEY_SOFT_AP_SSIDS, set())
    }

    fun onWifiNetworkDisabled(ssid: SSID) {
        log.v("onWifiNetworkDisabled() $ssid")
        val ssids = set(loadSSIDsWithKey(KEY_DISABLED_WIFI_SSIDS))
        ssids.add(ssid)
        saveWithKey(KEY_DISABLED_WIFI_SSIDS, ssids)
    }

    fun reenableWifiNetworks() {
        log.v("reenableWifiNetworks()")
        for (ssid in loadSSIDsWithKey(KEY_DISABLED_WIFI_SSIDS)) {
            wifiFacade.reenableNetwork(ssid)
        }
        saveWithKey(KEY_DISABLED_WIFI_SSIDS, set())
    }


    private fun loadSSIDsWithKey(key: String): Set<SSID> {
        return Funcy.transformSet(prefs.getStringSet(key, set())) { SSID.from(it) }
    }

    @SuppressLint("CommitPrefEdits")
    private fun saveWithKey(key: String, ssids: Set<SSID>) {
        val asStrings = transformSet<SSID, String>(ssids) { it.toString() }
        prefs.edit()
                .putStringSet(key, asStrings)
                .apply()
    }

    companion object {
        private val log = TLog.get(SoftAPConfigRemover::class.java)

        private const val PREFS_SOFT_AP_NETWORK_REMOVER = "PREFS_SOFT_AP_NETWORK_REMOVER"
        private const val KEY_SOFT_AP_SSIDS = "KEY_SOFT_AP_SSIDS"
        private const val KEY_DISABLED_WIFI_SSIDS = "KEY_DISABLED_WIFI_SSIDS"
    }
}
