package io.particle.android.sdk.utils

import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.os.Parcel
import android.os.Parcelable
import java.util.*


/**
 * Simple value wrapper for SSID strings.  Eliminates case comparison issues and the quoting
 * nonsense introduced by [android.net.wifi.WifiConfiguration.SSID] (and potentially elsewhere)
 */
class SSID private constructor(private val ssidString: String) : Comparable<SSID>, Parcelable {

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return ssidString
    }

    fun inQuotes(): String {
        return "\"" + ssidString + "\""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val ssid = other as SSID?

        return ssidString.equals(ssid!!.ssidString, ignoreCase = true)
    }

    override fun hashCode(): Int {
        return ssidString.toLowerCase(Locale.ROOT).hashCode()
    }

    override fun compareTo(other: SSID): Int {
        return ssidString.compareTo(other.ssidString, ignoreCase = true)
    }


    //region Parcelable noise
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(ssidString)
    }


    //endregion
    companion object CREATOR : Parcelable.Creator<SSID> {
        override fun createFromParcel(parcel: Parcel): SSID {
            return SSID(parcel)
        }

        override fun newArray(size: Int): Array<SSID?> {
            return arrayOfNulls(size)
        }

        fun from(rawSsidString: String?): SSID {
            return SSID(deQuotifySsid(rawSsidString!!))
        }

        fun from(wifiInfo: WifiInfo?): SSID {
            return from(wifiInfo?.ssid)
        }

        fun from(wifiConfiguration: WifiConfiguration): SSID {
            return from(wifiConfiguration.SSID)
        }

        fun from(scanResult: ScanResult): SSID {
            return SSID.from(scanResult.SSID)
        }

        private fun deQuotifySsid(SSID: String): String {
            var mutableSSID: String
            val quoteMark = "\""
            mutableSSID = ParticleDeviceSetupInternalStringUtils.removeStart(SSID, quoteMark)
            mutableSSID = ParticleDeviceSetupInternalStringUtils.removeEnd(mutableSSID, quoteMark)
            return mutableSSID
        }
    }
}
