package io.particle.android.sdk.devicesetup.commands

import android.support.annotation.CheckResult
import com.google.gson.Gson
import io.particle.android.sdk.utils.EZ
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import okio.BufferedSink
import okio.Okio
import java.io.IOException
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory


class CommandClient internal constructor(private val ipAddress: String, private val port: Int, private val socketFactory: SocketFactory) {

    @Throws(IOException::class)
    fun sendCommand(command: Command) {
        sendAndMaybeReceive(command, Void::class.java)
    }

    @CheckResult
    @Throws(IOException::class)
    fun <T> sendCommand(command: Command, responseType: Class<T>): T? {
        return sendAndMaybeReceive(command, responseType)
    }


    @Throws(IOException::class)
    private fun <T> sendAndMaybeReceive(command: Command, responseType: Class<T>): T? {
        log.i("Preparing to send command '" + command.commandName + "'")
        val commandData = buildCommandData(command)

        var buffer: BufferedSink? = null
        try {
            // send command
            val socket = socketFactory.createSocket(ipAddress, port)
            buffer = wrapSocket(socket, DEFAULT_TIMEOUT_SECONDS)
            log.d("Writing command data")
            buffer.writeUtf8(commandData)
            buffer.flush()

            // if no response defined, just exit early.
            if (responseType == Void::class.java) {
                log.d("Done.")
                return null
            }

            return readResponse(socket, responseType, DEFAULT_TIMEOUT_SECONDS)

        } finally {
            EZ.closeThisThingOrMaybeDont(buffer)
        }
    }

    @Throws(IOException::class)
    private fun wrapSocket(socket: Socket, timeoutValueInSeconds: Int): BufferedSink {
        val sink = Okio.buffer(Okio.sink(socket))
        sink.timeout().timeout(timeoutValueInSeconds.toLong(), TimeUnit.SECONDS)
        return sink
    }

    private fun buildCommandData(command: Command): String {
        val commandData = StringBuilder()
                .append(command.commandName)
                .append("\n")

        val commandArgs = command.argsAsJsonString(gson)
        if (truthy(commandArgs)) {
            commandData.append(commandArgs?.length)
            commandData.append("\n\n")
            commandData.append(commandArgs)
        } else {
            commandData.append("0\n\n")
        }

        val built = commandData.toString()
        log.i("*** BUILT COMMAND DATA: '" + CommandClientUtils.escapeJava(built) + "'")
        return built
    }

    @Throws(IOException::class)
    private fun <T> readResponse(socket: Socket, responseType: Class<T>, timeoutValueInSeconds: Int): T {
        val buffer = Okio.buffer(Okio.source(socket))
        buffer.timeout().timeout(timeoutValueInSeconds.toLong(), TimeUnit.SECONDS)

        log.d("Reading response data...")
        var line: String
        do {
            // read (and throw away, for now) any headers
            line = buffer.readUtf8LineStrict()
        } while (truthy(line))

        val responseData = buffer.readUtf8()

        log.d("Command response (raw): " + CommandClientUtils.escapeJava(responseData))
        val tee = gson.fromJson(responseData, responseType)
        log.d("Command response: $tee")
        EZ.closeThisThingOrMaybeDont(buffer)
        return tee
    }

    companion object {
        internal const val DEFAULT_TIMEOUT_SECONDS = 10

        private val log = TLog.get(CommandClient::class.java)
        private val gson = Gson()
    }

}
