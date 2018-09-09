package io.particle.android.sdk.devicesetup

import android.os.Parcel
import android.os.Parcelable

class SetupResult : Parcelable {
    private val wasSuccessful: Boolean
    private val configuredDeviceId: String?

    constructor(wasSuccessful: Boolean, configuredDeviceId: String?) {
        this.wasSuccessful = wasSuccessful
        this.configuredDeviceId = configuredDeviceId
    }

    fun wasSuccessful(): Boolean {
        return wasSuccessful
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(if (wasSuccessful) 1 else 0)
        dest.writeString(configuredDeviceId)
    }

    private constructor(source: Parcel) {
        wasSuccessful = source.readInt() == 1
        configuredDeviceId = source.readString()
    }

    companion object CREATOR : Parcelable.Creator<SetupResult> {
        override fun createFromParcel(parcel: Parcel): SetupResult {
            return SetupResult(parcel)
        }

        override fun newArray(size: Int): Array<SetupResult?> {
            return arrayOfNulls(size)
        }
    }
}
