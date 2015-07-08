package io.particle.android.sdk.devicesetup.setupsteps;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.google.common.base.Preconditions;

import io.particle.android.sdk.devicesetup.ui.DeviceSetupState;
import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.WiFi;

import static io.particle.android.sdk.utils.Py.list;


public class WaitForDisconnectionFromDeviceStep extends SetupStep {

    private final Context ctx;
    private final String softApName;
    private final WifiManager wifiManager;

    private boolean wasDisconnected = false;

    public WaitForDisconnectionFromDeviceStep(StepConfig stepConfig, String softApSSID,
                                              Context ctx) {
        super(stepConfig);

        Preconditions.checkNotNull(softApSSID, "softApSSID cannot be null.");

        this.ctx = ctx;
        this.softApName = softApSSID;
        this.wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean isStepFulfilled() {
        return wasDisconnected;
    }

    @Override
    protected void onRunStep() throws SetupStepException, SetupProcessException {
        for (int i = 0; i <= 5; i++) {
            if (isConnectedToSoftAp()) {
                // wait and try again
                EZ.threadSleep(200);
            } else {
                EZ.threadSleep(1000);
                // success, no longer connected.
                wasDisconnected = true;
                if (EZ.isUsingOlderWifiStack()) {
                    // for some reason Lollipop doesn't need this??
                    reenablePreviousWifi();
                }
                return;
            }
        }

        // Still connected after the above completed: fail
        throw new SetupStepException("Not disconnected from soft AP");
    }

    private void reenablePreviousWifi() {
        String prevSSID = DeviceSetupState.previouslyConnectedWifiNetwork;

        for (String ssid : list(prevSSID, WiFi.enQuotifySsid(prevSSID))) {
            int netId = WiFi.getConfiguredNetworkId(ssid, ctx);
            log.d("Found ID " + netId + " for network " + ssid);
            if (netId != -1) {
                wifiManager.enableNetwork(netId, false);
            }
        }

        wifiManager.reassociate();
    }

    private boolean isConnectedToSoftAp() {
        String currentlyConnectedSSID = WiFi.getCurrentlyConnectedSSID(ctx);
        log.d("Currently connected SSID: " + currentlyConnectedSSID);
        return softApName.equalsIgnoreCase(currentlyConnectedSSID);
    }

}
