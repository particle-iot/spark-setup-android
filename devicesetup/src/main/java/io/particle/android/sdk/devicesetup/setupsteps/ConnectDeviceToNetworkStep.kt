package io.particle.android.sdk.devicesetup.setupsteps

import io.particle.android.sdk.devicesetup.commands.CommandClient
import io.particle.android.sdk.devicesetup.commands.ConnectAPCommand
import java.io.IOException


class ConnectDeviceToNetworkStep internal constructor(stepConfig: StepConfig,
                                                      private val commandClient: CommandClient,
                                                      private val workerThreadApConnector: SetupStepApReconnector) : SetupStep(stepConfig) {
    override val isStepFulfilled: Boolean
        get() = commandSent

    @Volatile
    private var commandSent = false

    @Throws(SetupStepException::class)
    override fun onRunStep() {
        try {
            log.d("Ensuring connection to AP")
            workerThreadApConnector.ensureConnectionToSoftAp()

            log.d("Sending connect-ap command")
            val response = commandClient.sendCommand(
                    // FIXME: is hard-coding zero here correct?  If so, document why
                    ConnectAPCommand(0), ConnectAPCommand.Response::class.java)
            if (!response!!.isOK) {
                throw SetupStepException("ConnectAPCommand returned non-zero response code: " + response.responseCode)
            }

            commandSent = true

        } catch (e: IOException) {
            throw SetupStepException(e)
        }

    }

}
