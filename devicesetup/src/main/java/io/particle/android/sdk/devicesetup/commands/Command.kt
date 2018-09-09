package io.particle.android.sdk.devicesetup.commands

import com.google.gson.Gson


abstract class Command {

    abstract val commandName: String

    // override if you want a different implementation
    open fun argsAsJsonString(gson: Gson): String? {
        return gson.toJson(this)
    }
}
