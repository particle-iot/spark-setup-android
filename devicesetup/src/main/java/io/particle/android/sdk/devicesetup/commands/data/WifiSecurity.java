package io.particle.android.sdk.devicesetup.commands.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public enum WifiSecurity {

    OPEN(0), // Unsecured
    WEP_PSK(1), // WEP Security with open authentication
    WEP_SHARED(0x8001), // WEP Security with shared authentication
    WPA_TKIP_PSK(0x00200002), // WPA Security with TKIP
    WPA_AES_PSK(0x00200004), // WPA Security with AES
    WPA_MIXED_PSK(0x00200006), // WPA Security with AES & TKIP
    WPA2_AES_PSK(0x00400004), // WPA2 Security with AES
    WPA2_TKIP_PSK(0x00400002), // WPA2 Security with TKIP
    WPA2_MIXED_PSK(0x00400006); // WPA2 Security with AES & TKIP


    static final ImmutableMap<Integer, WifiSecurity> fromIntMap;

    static {
        ImmutableMap.Builder<Integer, WifiSecurity> builder = ImmutableMap.builder();
        fromIntMap = builder
                .put(OPEN.asInt(), OPEN)
                .put(WEP_PSK.asInt(), WEP_PSK)
                .put(WEP_SHARED.asInt(), WEP_SHARED)
                .put(WPA_TKIP_PSK.asInt(), WPA_TKIP_PSK)
                .put(WPA_MIXED_PSK.asInt(), WPA_MIXED_PSK)
                .put(WPA_AES_PSK.asInt(), WPA_AES_PSK)
                .put(WPA2_AES_PSK.asInt(), WPA2_AES_PSK)
                .put(WPA2_TKIP_PSK.asInt(), WPA2_TKIP_PSK)
                .put(WPA2_MIXED_PSK.asInt(), WPA2_MIXED_PSK)
                .build();
    }

    private final int intValue;

    WifiSecurity(int intValue) {
        this.intValue = intValue;
    }

    private static final int ENTERPRISE_ENABLED_MASK = 0x02000000;

    // FIXME: accommodate this better
    public static boolean isEnterpriseNetwork(int value) {
        return (ENTERPRISE_ENABLED_MASK & value) != 0;
    }

    public static WifiSecurity fromInteger(Integer value) {
        Preconditions.checkNotNull(value);
        Preconditions.checkArgument(fromIntMap.containsKey(value), "Value not found in map: " + value);

        return fromIntMap.get(value);
    }

    public int asInt() {
        return intValue;
    }

}
