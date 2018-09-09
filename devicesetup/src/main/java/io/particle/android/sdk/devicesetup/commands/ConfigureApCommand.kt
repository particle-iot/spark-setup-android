package io.particle.android.sdk.devicesetup.commands

import com.google.gson.annotations.SerializedName

import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity

import io.particle.android.sdk.utils.Py.all
import io.particle.android.sdk.utils.Py.truthy


/**
 * Configure the access point details to connect to when connect-ap is called. The AP doesn't have
 * to be in the list from scan-ap, allowing manual entry of hidden networks.
 */
class ConfigureApCommand
private constructor(val idx: Int?,
                    val ssid: String?,
                    @field:SerializedName("pwd")
                    val encryptedPasswordHex: String?,
                    wifiSecurityType: WifiSecurity,
                    @field:SerializedName("ch")
                    val channel: Int?) : Command() {

    @SerializedName("sec")
    val wifiSecurityType: Int?

    override val commandName: String
        get() = "configure-ap"

    init {
        this.wifiSecurityType = wifiSecurityType.asInt()
    }


    class Response(@field:SerializedName("r")
                   val responseCode: Int?  // 0 == OK, non-zero == problem with index/data
    ) {

        // FIXME: do this for the other ones with just the "responseCode" field
        val isOk: Boolean
            get() = responseCode == 0

        override fun toString(): String {
            return "Response{" +
                    "responseCode=" + responseCode +
                    '}'.toString()
        }
    }


    class Builder {
        private var idx: Int? = null
        private var ssid: String? = null
        private var encryptedPasswordHex: String? = null
        private var securityType: WifiSecurity? = null
        private var channel: Int? = null

        fun setIdx(idx: Int): Builder {
            this.idx = idx
            return this
        }

        fun setSsid(ssid: String): Builder {
            this.ssid = ssid
            return this
        }

        fun setEncryptedPasswordHex(encryptedPasswordHex: String): Builder {
            this.encryptedPasswordHex = encryptedPasswordHex
            return this
        }

        fun setSecurityType(securityType: WifiSecurity): Builder {
            this.securityType = securityType
            return this
        }

        fun setChannel(channel: Int): Builder {
            this.channel = channel
            return this
        }

        fun build(): ConfigureApCommand {
            if (!all(ssid, securityType) || truthy(encryptedPasswordHex) && securityType === WifiSecurity.OPEN) {
                throw IllegalArgumentException(
                        "One or more required arguments was not set on ConfigureApCommand")
            }
            if (idx == null) {
                idx = 0
            }
            return ConfigureApCommand(idx!!, ssid, encryptedPasswordHex, securityType!!, channel!!)
        }
    }

    companion object {

        fun newBuilder(): Builder {
            return Builder()
        }
    }

}
