package io.particle.android.sdk.devicesetup.setupsteps


import io.particle.android.sdk.devicesetup.commands.CommandClient
import io.particle.android.sdk.devicesetup.commands.ConfigureApCommand
import io.particle.android.sdk.devicesetup.commands.ScanApCommand
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity
import io.particle.android.sdk.utils.Crypto
import java.io.IOException
import java.security.PublicKey


class ConfigureAPStep internal constructor(stepConfig: StepConfig, private val commandClient: CommandClient,
                                           private val workerThreadApConnector: SetupStepApReconnector,
                                           private val networkToConnectTo: ScanApCommand.Scan,
                                           private val networkSecretPlaintext: String,
                                           private val publicKey: PublicKey) : SetupStep(stepConfig) {
    override val isStepFulfilled: Boolean
        get() = commandSent

    @Volatile
    private var commandSent = false

    @Throws(SetupStepException::class)
    override fun onRunStep() {
        val wifiSecurity = WifiSecurity.fromInteger(networkToConnectTo.wifiSecurityType)
        val builder = ConfigureApCommand.newBuilder()
                .setSsid(networkToConnectTo.ssid)
                .setSecurityType(wifiSecurity)
                .setChannel(networkToConnectTo.channel!!)
                .setIdx(0)
        if (wifiSecurity !== WifiSecurity.OPEN) {
            try {
                builder.setEncryptedPasswordHex(
                        Crypto.encryptAndEncodeToHex(networkSecretPlaintext, publicKey))
            } catch (e: Crypto.CryptoException) {
                // FIXME: try to throw a more specific exception here.
                // Don't throw SetupException here -- if this is failing, it's not
                // going to get any better by the running this SetupStep again, and
                // it can really only fail if the surrounding app code is doing something
                // wrong.  To wit: you *want* the app to crash here (or at least
                // throw out a dialog saying "horrible thing happened!  horrible error
                // code: ..." and then return to a safe "default" activity.
                throw RuntimeException("Error encrypting network credentials", e)
            }

        }
        val command = builder.build()

        try {
            log.d("Ensuring connection to AP")
            workerThreadApConnector.ensureConnectionToSoftAp()

            val response = commandClient.sendCommand(
                    command, ConfigureApCommand.Response::class.java)
            if (!response!!.isOk) {
                throw SetupStepException("Error response code " + response.responseCode +
                        " while configuring device")
            }
            log.d("Configure AP command returned: " + response.responseCode!!)
            commandSent = true

        } catch (e: IOException) {
            throw SetupStepException(e)
        }

    }

}
