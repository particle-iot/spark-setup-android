package io.particle.android.sdk.devicesetup.setupsteps;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.utils.EZ;


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
        this.matchesSoftApSSID = new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return softApName.equalsIgnoreCase(input);
            }
        };
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
        ImmutableList<String> scansPlusConnectedSsid = FluentIterable.from(wifiManager.getScanResults())
                .transform(toSSID)
                .toList();
        log.d("scansPlusConnectedSsid: " + scansPlusConnectedSsid);
        log.d("Soft AP we're looking for: " + softApName);
        Optional<String> matchingSSID = FluentIterable.from(wifiManager.getScanResults())
                .transform(toSSID)
                .firstMatch(matchesSoftApSSID);
        log.d("Matching SSID?: " + matchingSSID);
        return matchingSSID.isPresent();
    }


    private static final Function<ScanResult, String> toSSID = new Function<ScanResult, String>() {
        @Override
        public String apply(ScanResult input) {
            return (input == null) ? null : input.SSID;
        }
    };
}
