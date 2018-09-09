package io.particle.android.sdk.devicesetup


import io.particle.android.sdk.devicesetup.setupsteps.SetupStep

class SetupProcessException(msg: String, val failedStep: SetupStep) : Exception(msg)
