package io.particle.android.sdk.devicesetup.ui


import io.particle.android.sdk.utils.SSID
import java.security.PublicKey
import java.util.concurrent.ConcurrentSkipListSet

// FIXME: Statically defined, global, mutable state...  refactor this thing into oblivion soon.
object DeviceSetupState {

    internal val claimedDeviceIds: MutableSet<String> = ConcurrentSkipListSet()
    @Volatile
    var previouslyConnectedWifiNetwork: SSID? = null
    @Volatile
    internal var claimCode: String? = null
    @Volatile
    internal var publicKey: PublicKey? = null
    @Volatile
    internal var deviceToBeSetUpId: String? = null

    internal fun reset() {
        claimCode = null
        claimedDeviceIds.clear()
        publicKey = null
        deviceToBeSetUpId = null
        previouslyConnectedWifiNetwork = null
    }
}
