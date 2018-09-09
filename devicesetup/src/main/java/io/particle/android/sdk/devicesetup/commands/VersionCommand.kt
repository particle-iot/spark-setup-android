package io.particle.android.sdk.devicesetup.commands

import com.google.gson.annotations.SerializedName


class VersionCommand : NoArgsCommand() {

    override val commandName: String
        get() = "version"

    data class Response(@field:SerializedName("v") val version: Int)

}
