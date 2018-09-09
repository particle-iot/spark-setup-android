package io.particle.android.sdk.devicesetup.setupsteps

import io.particle.android.sdk.devicesetup.SetupProcessException
import io.particle.android.sdk.devicesetup.ui.DeviceSetupState
import io.particle.android.sdk.utils.EZ
import io.particle.android.sdk.utils.Preconditions
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.WifiFacade


class WaitForDisconnectionFromDeviceStep internal constructor(stepConfig: StepConfig, private val softApName: SSID, private val wifiFacade: WifiFacade) : SetupStep(stepConfig) {

    override var isStepFulfilled = false
        private set

    private val isConnectedToSoftAp: Boolean
        get() {
            val currentlyConnectedSSID = wifiFacade.currentlyConnectedSSID
            log.d("Currently connected SSID: " + currentlyConnectedSSID!!)
            return softApName == currentlyConnectedSSID
        }

    init {
        Preconditions.checkNotNull(softApName, "softApSSID cannot be null.")
    }

    @Throws(SetupStepException::class, SetupProcessException::class)
    override fun onRunStep() {
        try {
            for (i in 0..5) {
                if (isConnectedToSoftAp) {
                    // wait and try again
                    EZ.threadSleep(200)
                } else {
                    EZ.threadSleep(1000)
                    // success, no longer connected.
                    isStepFulfilled = true
                    if (EZ.isUsingOlderWifiStack()) {
                        // for some reason Lollipop doesn't need this??
                        reenablePreviousWifi()
                    }
                    return
                }
            }
        } catch (ignore: NullPointerException) {
            // exception thrown below
        }

        // Still connected after the above completed: fail
        throw SetupStepException("Not disconnected from soft AP")
    }

    private fun reenablePreviousWifi() {
        val prevSSID = DeviceSetupState.previouslyConnectedWifiNetwork
        wifiFacade.reenableNetwork(prevSSID!!)
        wifiFacade.reassociate()
    }

}
