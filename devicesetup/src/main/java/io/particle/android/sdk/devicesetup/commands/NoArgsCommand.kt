package io.particle.android.sdk.devicesetup.commands

import com.google.gson.Gson

/**
 * Convenience class for commands with no argument data
 */
abstract class NoArgsCommand : Command() {

    override fun argsAsJsonString(gson: Gson): String? {
        // this command has no argument data
        return null
    }
}
