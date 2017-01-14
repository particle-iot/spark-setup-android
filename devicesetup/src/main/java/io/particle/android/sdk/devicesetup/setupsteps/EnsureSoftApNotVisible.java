package io.particle.android.sdk.devicesetup.setupsteps;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.util.List;

import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.Funcy;
import io.particle.android.sdk.utils.Funcy.Predicate;
import io.particle.android.sdk.utils.Preconditions;


public class EnsureSoftApNotVisible extends SetupStep {

    private final WifiManager wifiManager;
    private final String softApName;
    private final Predicate<String> matchesSoftApSSID;

    private boolean wasFulfilledOnce = false;

    public EnsureSoftApNotVisible(StepConfig stepConfig, String softApSSID, Context ctx) {
        super(stepConfig);

        Preconditions.checkNotNull(softApSSID, "softApSSID cannot be null.");

        this.wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        this.softApName = softApSSID;
        this.matchesSoftApSSID = softApName::equalsIgnoreCase;
    }

    @Override
    public boolean isStepFulfilled() {
        return wasFulfilledOnce && !isSoftApVisible();
    }

    @Override
    protected void onRunStep() throws SetupStepException, SetupProcessException {
        if (!wasFulfilledOnce) {
            onStepNeverYetFulfilled();

        } else {
            onStepPreviouslyFulfilled();
        }
    }

    // Before the soft AP disappears for the FIRST time, be lenient in allowing for retries if
    // it fails to disappear, since we don't have a good idea of why it's failing, so just throw
    // SetupStepException.  (But see onStepPreviouslyFulfilled())
    private void onStepNeverYetFulfilled() throws SetupStepException {
        for (int i = 0; i < 16; i++) {
            if (!isSoftApVisible()) {
                // it's gone!
                wasFulfilledOnce = true;
                return;
            }

            if (i % 6 == 0) {
                wifiManager.startScan();
            }

            EZ.threadSleep(250);
        }
        throw new SetupStepException("Wi-Fi credentials appear to be incorrect or an error has occurred");
    }

    // If this step was previously fulfilled, i.e.: the soft AP was gone, and now it's visible again,
    // this almost certainly means the device was given invalid Wi-Fi credentials, so we should
    // fail the whole setup process immediately.
    private void onStepPreviouslyFulfilled() throws SetupProcessException {
        if (isSoftApVisible()) {
            throw new SetupProcessException(
                    "Soft AP visible again; Wi-Fi credentials may be incorrect", this);
        }
    }

    private boolean isSoftApVisible() {
        List<String> scansPlusConnectedSsid = Funcy.transformList(wifiManager.getScanResults(),
                input -> (input == null) ? null : input.SSID);
        log.d("scansPlusConnectedSsid: " + scansPlusConnectedSsid);
        log.d("Soft AP we're looking for: " + softApName);

        String firstMatch = Funcy.findFirstMatch(scansPlusConnectedSsid, matchesSoftApSSID);
        log.d("Matching SSID?: " + firstMatch);
        return firstMatch != null;
    }

}
