package io.particle.android.sdk.accountsetup

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.ParticleUi
import kotlinx.android.synthetic.main.activity_password_reset.*
import kotlinx.android.synthetic.main.activity_password_reset.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import javax.inject.Inject

/**
 * Created by Julius.
 */
class PasswordResetFragment : BaseFragment() {

    @Inject
    lateinit var sparkCloud: ParticleCloud

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.activity_password_reset, container, false)

        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        SEGAnalytics.screen("Auth: Forgot password screen")
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view)

        view.action_cancel.setOnClickListener {
            Navigation.findNavController(view).navigateUp()
        }

        view.action_reset_password.setOnClickListener {
            onPasswordResetClicked()
        }

        view.email.setText(arguments!!.getString(EXTRA_EMAIL))
        return view
    }

    private fun onPasswordResetClicked() {
        SEGAnalytics.track("Auth: Request password reset")
        val emailValue = email.text.toString()
        if (isEmailValid(emailValue)) {
            performReset()
        } else {
            AlertDialog.Builder(context!!)
                    .setTitle(getString(R.string.reset_password_dialog_title))
                    .setMessage(getString(R.string.reset_paassword_dialog_please_enter_a_valid_email))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        email.requestFocus()
                    }
                    .show()
        }
    }

    private fun performReset() {
        ParticleUi.showParticleButtonProgress(view, R.id.action_reset_password, true)

        launch(UI) {
            try {
                withContext(CommonPool) {
                    sparkCloud.requestPasswordReset(email.text.toString())
                }

                onResetAttemptFinished("Instructions for how to reset your password will be sent " +
                        "to the provided email address.  Please check your email and continue " +
                        "according to instructions.")
            } catch (ex: ParticleCloudException) {
                log.d("onFailed(): " + ex.message)
                onResetAttemptFinished("Could not find a user with supplied email address, please " + " check the address supplied or create a new user via the signup screen")
            }
            ParticleUi.showParticleButtonProgress(view, R.id.action_reset_password, false)
        }
    }

    private fun onResetAttemptFinished(content: String) {
        AlertDialog.Builder(context!!)
                .setMessage(content)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    private fun isEmailValid(email: String): Boolean {
        return truthy(email) && email.contains("@")
    }

    companion object {
        const val EXTRA_EMAIL = "EXTRA_EMAIL"

        private val log = TLog.get(PasswordResetFragment::class.java)
    }

}
