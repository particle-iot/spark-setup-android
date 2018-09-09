package io.particle.android.sdk.devicesetup.setupsteps


open class SetupStepException : Exception {

    constructor(msg: String, throwable: Throwable) : super(msg, throwable)

    constructor(msg: String) : super(msg)

    constructor(throwable: Throwable) : super(throwable)

}
