package io.particle.android.sdk.devicesetup.model

import io.particle.android.sdk.utils.SSID


interface WifiNetwork {
    val ssid: SSID?

    val isSecured: Boolean
}
