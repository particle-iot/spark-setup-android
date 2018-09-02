package io.particle.android.sdk.devicesetup.model

import android.net.wifi.ScanResult

import io.particle.android.sdk.utils.SSID

import io.particle.android.sdk.utils.Py.set


// FIXME: this naming... is not ideal.
class ScanResultNetwork(private val scanResult: ScanResult) : WifiNetwork {
    override val ssid: SSID? = SSID.from(scanResult.SSID)

    override// <sad trombone>
    // this seems like a bad joke of an "API", but this is basically what
    // Android does internally (see: http://goo.gl/GCRIKi)
    val isSecured: Boolean
        get() {
            for (securityType in wifiSecurityTypes) {
                if (scanResult.capabilities.contains(securityType)) {
                    return true
                }
            }
            return false
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as ScanResultNetwork?

        return if (ssid != null) ssid == that!!.ssid else that!!.ssid == null
    }

    override fun hashCode(): Int {
        return ssid?.hashCode() ?: 0
    }

    companion object {

        private val wifiSecurityTypes = set("WEP", "PSK", "EAP")
    }
}
