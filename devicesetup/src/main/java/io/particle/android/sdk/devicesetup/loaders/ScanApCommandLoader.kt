package io.particle.android.sdk.devicesetup.loaders

import android.content.Context
import io.particle.android.sdk.devicesetup.commands.CommandClient
import io.particle.android.sdk.devicesetup.commands.ScanApCommand
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult
import io.particle.android.sdk.utils.BetterAsyncTaskLoader
import io.particle.android.sdk.utils.Py.set
import io.particle.android.sdk.utils.TLog
import java.io.IOException
import javax.annotation.ParametersAreNonnullByDefault


/**
 * Returns the results of the "scan-ap" command from the device.
 *
 *
 * Will return null if an exception is thrown when trying to send the command
 * and receive a reply from the device.
 */
@ParametersAreNonnullByDefault
class ScanApCommandLoader(context: Context, private val commandClient: CommandClient) : BetterAsyncTaskLoader<Set<ScanAPCommandResult>>(context) {
    private val accumulatedResults = set<ScanAPCommandResult>()

    override val loadedContent: Set<ScanAPCommandResult>
        get() = accumulatedResults

    override fun hasContent(): Boolean {
        return !accumulatedResults.isEmpty()
    }

    override fun onStartLoading() {
        super.onStartLoading()
        forceLoad()
    }

    override fun onStopLoading() {
        cancelLoad()
    }

    override fun loadInBackground(): Set<ScanAPCommandResult>? {
        return try {
            val response = commandClient.sendCommand(ScanApCommand(), ScanApCommand.Response::class.java)
            accumulatedResults.addAll(response!!.scans.map { ScanAPCommandResult(it) })
            log.d("Latest accumulated scan results: $accumulatedResults")
            set(accumulatedResults)

        } catch (e: IOException) {
            log.e("Error running scan-ap command: ", e)
            null
        }

    }

    companion object {

        private val log = TLog.get(ScanApCommandLoader::class.java)
    }

}
