package io.particle.android.sdk.devicesetup;

public class SetupResult {
    private boolean wasSuccessful;
    private String configuredDeviceId;

    public SetupResult(boolean wasSuccessful, String configuredDeviceId) {
        this.wasSuccessful = wasSuccessful;
        this.configuredDeviceId = configuredDeviceId;
    }

    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    public void setWasSuccessful(boolean wasSuccessful) {
        this.wasSuccessful = wasSuccessful;
    }

    public String getConfiguredDeviceId() {
        return configuredDeviceId;
    }

    public void setConfiguredDeviceId(String configuredDeviceId) {
        this.configuredDeviceId = configuredDeviceId;
    }
}
