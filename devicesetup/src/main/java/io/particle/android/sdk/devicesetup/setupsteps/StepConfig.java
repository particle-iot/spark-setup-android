package io.particle.android.sdk.devicesetup.setupsteps;

import io.particle.android.sdk.utils.Preconditions;


public class StepConfig {

    public final int maxAttempts;
    public final int stepId;
    public final int resultCode;

    private StepConfig(int maxAttempts, int stepId, int resultCode) {
        this.maxAttempts = maxAttempts;
        this.stepId = stepId;
        this.resultCode = resultCode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private int maxAttempts;
        private int stepId;
        private int resultCode;

        public Builder setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder setStepId(int stepId) {
            this.stepId = stepId;
            return this;
        }

        public Builder setResultCode(int resultCode) {
            this.resultCode = resultCode;
            return this;
        }

        public StepConfig build() {
            Preconditions.checkArgument(maxAttempts > 0, "Max attempts must be > 0");
            Preconditions.checkArgument(stepId != 0, "Step ID cannot be unset or set to 0");
            Preconditions.checkArgument(resultCode != 0, "Result code cannot be unset or set to 0");
            return new StepConfig(maxAttempts, stepId, resultCode);
        }
    }

}
