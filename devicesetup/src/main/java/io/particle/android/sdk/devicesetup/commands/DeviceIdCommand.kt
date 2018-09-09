package io.particle.android.sdk.devicesetup.commands

import com.google.gson.annotations.SerializedName

/**
 * Retrieves the unique device ID as a 24-digit hex string
 */
class DeviceIdCommand : NoArgsCommand() {

    override val commandName: String
        get() = "device-id"

    data class Response(@field:SerializedName("id")
                        val deviceIdHex: String,
                        @field:SerializedName("c")
                        val isClaimed: Int)
}
