package io.particle.android.sdk.devicesetup.commands


import com.google.gson.annotations.SerializedName
import java.util.*

class ScanApCommand : NoArgsCommand() {

    override val commandName: String
        get() = "scan-ap"

    data class Response(// using an array here instead of a generic
            // collection makes Gson usage simpler
            val scans: Array<Scan>) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Response

            if (!Arrays.equals(scans, other.scans)) return false

            return true
        }

        override fun hashCode(): Int {
            return Arrays.hashCode(scans)
        }
    }


    data class Scan(val ssid: String,
                    @field:SerializedName("sec")
                    val wifiSecurityType: Int?,
                    @field:SerializedName("ch")
                    val channel: Int?)

}
