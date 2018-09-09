package io.particle.android.sdk.devicesetup.setupsteps

import android.os.AsyncTask

import io.particle.android.sdk.devicesetup.SetupProcessException
import io.particle.android.sdk.utils.EZ
import io.particle.android.sdk.utils.TLog


abstract class SetupStepsRunnerTask(private val steps: List<SetupStep>, private val maxOverallAttempts: Int) : AsyncTask<Void, StepProgress, SetupProcessException>() {

    private val log = TLog.get(javaClass)

    public override fun doInBackground(vararg voids: Void): SetupProcessException? {
        var attempts = 0
        // We should never hit this limit, but just in case, we want to
        // avoid an infinite loop
        while (attempts < maxOverallAttempts) {
            attempts++
            try {
                runSteps()
                // we got all the way through the steps, break out of the loop!
                return null
            } catch (e: SetupStepException) {
                log.w("Setup step failed: " + e.message)

            } catch (e: SetupProcessException) {
                return e
            }
        }

        return SetupProcessException("(Unknown setup error)", null!!)
    }

    @Throws(SetupStepException::class, SetupProcessException::class)
    private fun runSteps() {
        for (step in steps) {

            throwIfCancelled()

            publishProgress(StepProgress(
                    step.stepConfig.stepId,
                    StepProgress.STARTING))

            try {
                EZ.threadSleep(1000)
                throwIfCancelled()

                step.runStep()

            } catch (e: SetupStepException) {
                // give it a moment before trying again.
                EZ.threadSleep(2000)
                throw e
            }

            publishProgress(StepProgress(
                    step.stepConfig.stepId,
                    StepProgress.SUCCEEDED))
        }
    }

    private fun throwIfCancelled() {
        // FIXME: while it's good that we handle being cancelled, this doesn't seem like
        // an ideal way to do it...
        if (isCancelled) {
            throw RuntimeException("Task was cancelled")
        }
    }
}
