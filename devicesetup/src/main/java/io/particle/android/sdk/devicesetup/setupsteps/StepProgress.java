package io.particle.android.sdk.devicesetup.setupsteps;


public class StepProgress {

    public static final int STARTING = 1;
    public static final int SUCCEEDED = 2;

    public final int stepId;
    public final int status;

    public StepProgress(int stepId, int status) {
        this.status = status;
        this.stepId = stepId;
    }
}
