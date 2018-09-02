package io.particle.android.sdk.devicesetup.commands.data


import android.util.SparseArray

import io.particle.android.sdk.utils.Preconditions


enum class WifiSecurity(private val intValue: Int) {

    OPEN(0), // Unsecured
    WEP_PSK(1), // WEP Security with open authentication
    WEP_SHARED(0x8001), // WEP Security with shared authentication
    WPA_TKIP_PSK(0x00200002), // WPA Security with TKIP
    WPA_AES_PSK(0x00200004), // WPA Security with AES
    WPA_MIXED_PSK(0x00200006), // WPA Security with AES & TKIP
    WPA2_AES_PSK(0x00400004), // WPA2 Security with AES
    WPA2_TKIP_PSK(0x00400002), // WPA2 Security with TKIP
    WPA2_MIXED_PSK(0x00400006);

    fun asInt(): Int {
        return intValue
    }

    companion object {
        // WPA2 Security with AES & TKIP
        private val fromIntMap: SparseArray<WifiSecurity> = SparseArray()

        init {
            fromIntMap.put(OPEN.asInt(), OPEN)
            fromIntMap.put(WEP_PSK.asInt(), WEP_PSK)
            fromIntMap.put(WEP_SHARED.asInt(), WEP_SHARED)
            fromIntMap.put(WPA_TKIP_PSK.asInt(), WPA_TKIP_PSK)
            fromIntMap.put(WPA_MIXED_PSK.asInt(), WPA_MIXED_PSK)
            fromIntMap.put(WPA_AES_PSK.asInt(), WPA_AES_PSK)
            fromIntMap.put(WPA2_AES_PSK.asInt(), WPA2_AES_PSK)
            fromIntMap.put(WPA2_TKIP_PSK.asInt(), WPA2_TKIP_PSK)
            fromIntMap.put(WPA2_MIXED_PSK.asInt(), WPA2_MIXED_PSK)
        }

        private const val ENTERPRISE_ENABLED_MASK = 0x02000000

        // FIXME: accommodate this better
        fun isEnterpriseNetwork(value: Int): Boolean {
            return ENTERPRISE_ENABLED_MASK and value != 0
        }

        fun fromInteger(value: Int?): WifiSecurity {
            Preconditions.checkNotNull(value!!)
            fromIntMap.indexOfKey(value)
            Preconditions.checkArgument(
                    fromIntMap.indexOfKey(value) >= 0,
                    "Value not found in map: $value")

            return fromIntMap.get(value)
        }
    }

}
