package io.particle.android.sdk.devicesetup.setupsteps

import io.particle.android.sdk.utils.Preconditions


class StepConfig private constructor(internal val maxAttempts: Int, val stepId: Int, val resultCode: Int) {

    class Builder {
        private var maxAttempts: Int = 0
        private var stepId: Int = 0
        private var resultCode: Int = 0

        internal fun setMaxAttempts(maxAttempts: Int): Builder {
            this.maxAttempts = maxAttempts
            return this
        }

        internal fun setStepId(stepId: Int): Builder {
            this.stepId = stepId
            return this
        }

        internal fun setResultCode(resultCode: Int): Builder {
            this.resultCode = resultCode
            return this
        }

        fun build(): StepConfig {
            Preconditions.checkArgument(maxAttempts > 0, "Max attempts must be > 0")
            Preconditions.checkArgument(stepId != 0, "Step ID cannot be unset or set to 0")
            Preconditions.checkArgument(resultCode != 0, "Result code cannot be unset or set to 0")
            return StepConfig(maxAttempts, stepId, resultCode)
        }
    }

    companion object {

        internal fun newBuilder(): Builder {
            return Builder()
        }
    }

}
