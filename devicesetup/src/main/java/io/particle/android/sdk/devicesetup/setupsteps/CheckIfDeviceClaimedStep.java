package io.particle.android.sdk.devicesetup.setupsteps;


import java.util.List;

import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;


public class CheckIfDeviceClaimedStep extends SetupStep {

    private final SparkCloud sparkCloud;
    private final String deviceBeingConfiguredId;
    // FIXME: this seems kind of lame, look into a diff approach to determine this
    private boolean needToClaimDevice;

    public CheckIfDeviceClaimedStep(StepConfig stepConfig, SparkCloud sparkCloud,
                                    String deviceBeingConfiguredId, boolean needToClaimDevice) {
        super(stepConfig);
        this.sparkCloud = sparkCloud;
        this.deviceBeingConfiguredId = deviceBeingConfiguredId;
        this.needToClaimDevice = needToClaimDevice;
    }

    @Override
    protected void onRunStep() throws SetupStepException {
        List<SparkDevice> devices;
        try {
            devices = sparkCloud.getDevices();
        } catch (SparkCloudException e) {
            throw new SetupStepException(e);
        }

        log.d("Got devices back from the cloud...");
        for (SparkDevice device : devices) {
            if (deviceBeingConfiguredId.equalsIgnoreCase(device.getID())) {
                log.d("Success, device " + device.getID() + " claimed!");
                needToClaimDevice = false;
                return;
            }
        }

        // device not found in the loop
        throw new SetupStepException("Device " + deviceBeingConfiguredId + " still not claimed.");
    }

    @Override
    public boolean isStepFulfilled() {
        return !needToClaimDevice;
    }

}
