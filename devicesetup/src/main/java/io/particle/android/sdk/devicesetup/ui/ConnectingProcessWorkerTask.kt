package io.particle.android.sdk.devicesetup.ui

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.navigation.Navigation
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.SetupProcessException
import io.particle.android.sdk.devicesetup.setupsteps.SetupStep
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsRunnerTask
import io.particle.android.sdk.devicesetup.setupsteps.StepProgress
import io.particle.android.sdk.utils.CoreNameGenerator
import io.particle.android.sdk.utils.Funcy
import io.particle.android.sdk.utils.Py.set
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.Ui
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Created by Julius.
 */

class ConnectingProcessWorkerTask internal constructor(activity: Activity, steps: List<SetupStep>, maxOverallAttempts: Int) : SetupStepsRunnerTask(steps, maxOverallAttempts) {

    @Inject
    lateinit var sparkCloud: ParticleCloud

    private val activityReference: WeakReference<Activity>
    private val deviceId: String
    private val tintedSpinner: Drawable
    private val tintedCheckmark: Drawable

    init {
        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .build()
                .inject(this)

        this.deviceId = DeviceSetupState.deviceToBeSetUpId!!
        this.activityReference = WeakReference(activity)
        this.tintedSpinner = Ui.getTintedDrawable(activity, R.drawable.progress_spinner, R.color.element_tint_color)
        this.tintedCheckmark = Ui.getTintedDrawable(activity, R.drawable.checkmark, R.color.element_tint_color)
    }

    override fun onProgressUpdate(vararg values: StepProgress) {
        val activity = activityReference.get() ?: return

        for (progress in values) {
            val v = activity.findViewById<View>(progress.stepId)
            if (v != null) {
                updateProgress(progress, v)
            }
        }
    }

    override fun onPostExecute(error: SetupProcessException?) {
        val resultCode: Int

        if (error != null) {
            resultCode = error.failedStep.stepConfig.resultCode

        } else {
            log.d("HUZZAH, VICTORY!")
            // FIXME: handle "success, no ownership" case
            resultCode = SuccessFragment.RESULT_SUCCESS

            launch(CommonPool) {
                try {
                    // collect a list of unique, non-null device names
                    val names = set(Funcy.transformList<ParticleDevice, String>(
                            sparkCloud.devices,
                            Funcy.notNull(),
                            Funcy.Func<ParticleDevice, String> { it.name },
                            Funcy.Predicate<String> { truthy(it) }
                    ))
                    val device = sparkCloud.getDevice(deviceId)
                    if (device != null && !truthy(device.name)) {
                        device.name = CoreNameGenerator.generateUniqueName(names)
                    }
                } catch (e: Exception) {
                    // FIXME: do real error handling here, and only
                    // handle ParticleCloudException instead of swallowing everything
                    e.printStackTrace()
                }
            }
        }

        val activity = activityReference.get()
        if (activity != null) {
            val bundle = Bundle()
            bundle.putInt(SuccessFragment.EXTRA_RESULT_CODE, resultCode)
            bundle.putString(SuccessFragment.EXTRA_DEVICE_ID, deviceId)
            Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.action_connectingFragment_to_successFragment, bundle)
        }
    }

    private fun updateProgress(progress: StepProgress, progressStepContainer: View) {
        val progBar = Ui.findView<ProgressBar>(progressStepContainer, R.id.spinner)
        val checkmark = Ui.findView<ImageView>(progressStepContainer, R.id.checkbox)

        // don't show the spinner again if we've already shown the checkmark,
        // regardless of the underlying state that might hide
        if (checkmark.visibility == View.VISIBLE) {
            return
        }

        progressStepContainer.visibility = View.VISIBLE

        if (progress.status == StepProgress.STARTING) {
            checkmark.visibility = View.GONE

            progBar.progressDrawable = tintedSpinner
            progBar.visibility = View.VISIBLE

        } else {
            progBar.visibility = View.GONE

            checkmark.setImageDrawable(tintedCheckmark)
            checkmark.visibility = View.VISIBLE
        }
    }

    companion object {
        private val log = TLog.get(ConnectingProcessWorkerTask::class.java)
    }
}