package io.particle.android.sdk.devicesetup.commands

import io.particle.android.sdk.devicesetup.commands.CommandClient.Companion.DEFAULT_TIMEOUT_SECONDS
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.WifiFacade

class CommandClientFactory {

    fun newClient(wifiFacade: WifiFacade, softApSSID: SSID?, ipAddress: String, port: Int): CommandClient {
        return CommandClient(ipAddress, port,
                NetworkBindingSocketFactory(wifiFacade, softApSSID, DEFAULT_TIMEOUT_SECONDS * 1000))
    }

    // FIXME: set these defaults in a resource file?
    fun newClientUsingDefaultsForDevices(wifiFacade: WifiFacade, softApSSID: SSID?): CommandClient {
        return newClient(wifiFacade, softApSSID, "192.168.0.1", 5609)
    }

}
