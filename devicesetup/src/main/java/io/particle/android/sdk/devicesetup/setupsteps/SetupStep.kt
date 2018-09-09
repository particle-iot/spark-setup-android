package io.particle.android.sdk.devicesetup.setupsteps

import android.annotation.SuppressLint
import android.support.annotation.AnyThread
import android.support.annotation.WorkerThread

import io.particle.android.sdk.devicesetup.SetupProcessException
import io.particle.android.sdk.utils.TLog


@WorkerThread
abstract class SetupStep(@get:AnyThread
                         val stepConfig: StepConfig) {

    val log: TLog = TLog.get(this.javaClass)
    @Volatile
    private var numAttempts: Int = 0

    abstract val isStepFulfilled: Boolean

    private val stepName: String
        get() = this.javaClass.simpleName


    @Throws(SetupStepException::class, SetupProcessException::class)
    protected abstract fun onRunStep()

    @Throws(SetupStepException::class, SetupProcessException::class)
    fun runStep() {
        if (isStepFulfilled) {
            log.i("Step $stepName already fulfilled, skipping...")
            return
        }
        if (numAttempts > stepConfig.maxAttempts) {
            @SuppressLint("DefaultLocale")
            val msg = String.format("Exceeded limit of %d retries for step %s",
                    stepConfig.maxAttempts, stepName)
            throw SetupProcessException(msg, this)
        } else {
            log.i("Running step $stepName")
            numAttempts++
            onRunStep()
        }
    }

    fun resetAttemptsCount() {
        numAttempts = 0
    }

}
