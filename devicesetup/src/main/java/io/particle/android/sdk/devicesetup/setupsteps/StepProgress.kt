package io.particle.android.sdk.devicesetup.setupsteps


class StepProgress internal constructor(val stepId: Int, val status: Int) {
    companion object {
        const val STARTING = 1
        const val SUCCEEDED = 2
    }
}
