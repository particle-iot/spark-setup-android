package io.particle.android.sdk.devicesetup.setupsteps

import io.particle.android.sdk.devicesetup.SetupProcessException
import io.particle.android.sdk.utils.*
import io.particle.android.sdk.utils.Funcy.Predicate
import io.particle.android.sdk.utils.Py.list


class EnsureSoftApNotVisible internal constructor(stepConfig: StepConfig,
                                                  private val softApName: SSID?,
                                                  private val wifiFacade: WifiFacade) : SetupStep(stepConfig) {
    override val isStepFulfilled: Boolean
        get() = wasFulfilledOnce && !isSoftApVisible

    private val matchesSoftApSSID: Predicate<SSID>

    private var wasFulfilledOnce = false

    private val isSoftApVisible: Boolean
        get() {
            val scansPlusConnectedSsid = list<SSID>()

            val currentlyConnected = wifiFacade.currentlyConnectedSSID
            if (currentlyConnected != null) {
                scansPlusConnectedSsid.add(currentlyConnected)
            }

            scansPlusConnectedSsid.addAll(wifiFacade.scanResults.map { SSID.from(it) })

            log.d("scansPlusConnectedSsid: $scansPlusConnectedSsid")
            log.d("Soft AP we're looking for: $softApName")

            val firstMatch = Funcy.findFirstMatch(scansPlusConnectedSsid, matchesSoftApSSID)
            log.d("Matching SSID result: '$firstMatch'")
            return firstMatch != null
        }

    init {
        Preconditions.checkNotNull(softApName, "softApSSID cannot be null.")
        this.matchesSoftApSSID = Predicate { softApName == it }
    }

    @Throws(SetupStepException::class, SetupProcessException::class)
    override fun onRunStep() {
        if (!wasFulfilledOnce) {
            onStepNeverYetFulfilled()

        } else {
            onStepPreviouslyFulfilled()
        }
    }

    // Before the soft AP disappears for the FIRST time, be lenient in allowing for retries if
    // it fails to disappear, since we don't have a good idea of why it's failing, so just throw
    // SetupStepException.  (But see onStepPreviouslyFulfilled())
    @Throws(SetupStepException::class)
    private fun onStepNeverYetFulfilled() {
        for (i in 0..15) {
            if (!isSoftApVisible) {
                // it's gone!
                wasFulfilledOnce = true
                return
            }

            if (i % 6 == 0) {
                wifiFacade.startScan()
            }

            EZ.threadSleep(250)
        }
        throw SetupStepException("Wi-Fi credentials appear to be incorrect or an error has occurred")
    }

    // If this step was previously fulfilled, i.e.: the soft AP was gone, and now it's visible again,
    // this almost certainly means the device was given invalid Wi-Fi credentials, so we should
    // fail the whole setup process immediately.
    @Throws(SetupProcessException::class)
    private fun onStepPreviouslyFulfilled() {
        if (isSoftApVisible) {
            throw SetupProcessException(
                    "Soft AP visible again; Wi-Fi credentials may be incorrect", this)
        }
    }

}
