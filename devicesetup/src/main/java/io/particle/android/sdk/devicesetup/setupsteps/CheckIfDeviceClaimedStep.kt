package io.particle.android.sdk.devicesetup.setupsteps


import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException


class CheckIfDeviceClaimedStep internal constructor(stepConfig: StepConfig,
                                                    private val sparkCloud: ParticleCloud,
                                                    private val deviceBeingConfiguredId: String) : SetupStep(stepConfig) {
    override val isStepFulfilled: Boolean
        get() = !needToClaimDevice

    private var needToClaimDevice = true

    @Throws(SetupStepException::class)
    override fun onRunStep() {
        val devices: List<ParticleDevice>
        try {
            devices = sparkCloud.devices
        } catch (e: ParticleCloudException) {
            throw SetupStepException(e)
        }

        log.d("Got devices back from the cloud...")
        for (device in devices) {
            if (deviceBeingConfiguredId.equals(device.id, ignoreCase = true)) {
                log.d("Success, device " + device.id + " claimed!")
                needToClaimDevice = false
                return
            }
        }

        // device not found in the loop
        throw SetupStepException("Device $deviceBeingConfiguredId still not claimed.")
    }

}
