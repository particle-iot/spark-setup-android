package io.particle.android.sdk.devicesetup.ui;


import java.security.PublicKey;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

// FIXME: Statically defined, global, mutable state...  refactor this thing into oblivion soon.
public class DeviceSetupState {

    static final Set<String> claimedDeviceIds = new ConcurrentSkipListSet<>();
    public static volatile String previouslyConnectedWifiNetwork;
    static volatile String claimCode;
    static volatile PublicKey publicKey;
    static volatile String deviceToBeSetUpId;
    static volatile boolean deviceNeedsToBeClaimed = true;

    static void reset() {
        claimCode = null;
        claimedDeviceIds.clear();
        publicKey = null;
        deviceToBeSetUpId = null;
        deviceNeedsToBeClaimed = true;
        previouslyConnectedWifiNetwork = null;
    }
}
