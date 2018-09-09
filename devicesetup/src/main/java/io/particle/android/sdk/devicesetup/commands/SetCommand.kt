package io.particle.android.sdk.devicesetup.commands

import com.google.gson.annotations.SerializedName

import io.particle.android.sdk.utils.Preconditions


class SetCommand(@field:SerializedName("k")
                 val key: String,
                 @field:SerializedName("v")
                 val value: String?) : Command() {

    override val commandName: String
        get() = "set"

    init {
        Preconditions.checkNotNull(key, "Key cannot be null")
        Preconditions.checkNotNull(value, "Value cannot be null")
    }

    data class Response(@field:SerializedName("r") val responseCode: Int)
}
