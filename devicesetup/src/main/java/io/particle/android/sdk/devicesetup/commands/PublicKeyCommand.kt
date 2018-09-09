package io.particle.android.sdk.devicesetup.commands

import com.google.gson.annotations.SerializedName

class PublicKeyCommand : NoArgsCommand() {

    override val commandName: String
        get() = "public-key"

    data class Response(@field:SerializedName("r")
                   val responseCode: Int, // Hex-encoded public key, in DER format
                   @field:SerializedName("b")
                   val publicKey: String)

}
