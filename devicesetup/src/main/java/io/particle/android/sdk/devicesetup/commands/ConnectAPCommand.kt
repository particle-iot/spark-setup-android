package io.particle.android.sdk.devicesetup.commands

import com.google.gson.annotations.SerializedName

/**
 * Connects to an AP previously configured with configure-ap. This disconnects the soft-ap after
 * the response code has been sent. Note that the response code doesn't indicate successful
 * connection to the AP, but only that the command was acknowledged and the AP will be
 * connected to after the result is sent to the client.
 *
 *
 * If the AP connection is unsuccessful, the soft-AP will be reinstated so the user can enter
 * new credentials/try again.
 */
data class ConnectAPCommand(@field:SerializedName("idx")
                       val index: Int) : Command() {

    override val commandName: String
        get() = "connect-ap"


    data class Response(@field:SerializedName("r")
                        val responseCode: Int  // 0 == OK, non-zero == problem with index/data
    ) {
        val isOK: Boolean
            get() = responseCode == 0
    }
}
