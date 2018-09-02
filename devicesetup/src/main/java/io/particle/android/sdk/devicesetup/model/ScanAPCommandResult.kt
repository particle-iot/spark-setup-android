package io.particle.android.sdk.devicesetup.model

import io.particle.android.sdk.devicesetup.commands.ScanApCommand
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity
import io.particle.android.sdk.utils.SSID


// FIXME: this naming is not ideal.
class ScanAPCommandResult(val scan: ScanApCommand.Scan) : WifiNetwork {
    override val ssid: SSID? = SSID.from(scan.ssid)
    override val isSecured: Boolean
        get() = scan.wifiSecurityType != WifiSecurity.OPEN.asInt()

    override fun toString(): String {
        return "ScanAPCommandResult{" +
                "scan=" + scan +
                '}'.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as ScanAPCommandResult?

        return if (ssid != null) ssid == that!!.ssid else that!!.ssid == null
    }

    override fun hashCode(): Int {
        return ssid?.hashCode() ?: 0
    }

}
