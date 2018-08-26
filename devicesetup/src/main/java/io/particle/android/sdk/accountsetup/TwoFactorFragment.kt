package io.particle.android.sdk.accountsetup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import javax.inject.Inject

import androidx.navigation.Navigation
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.ParticleUi
import io.particle.android.sdk.utils.ui.Ui
import kotlinx.android.synthetic.main.activity_two_factor.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

class TwoFactorFragment : BaseFragment() {

    /**
     * Keep track of the login job to ensure we can cancel it if requested, ensure against
     * duplicate requests, etc.
     */
    private var loginJob: Job? = null

    @Inject
    lateinit var sparkCloud: ParticleCloud

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.activity_two_factor, container, false)

        //dependency injection
        ParticleDeviceSetupLibrary
                .getInstance()
                .applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        SEGAnalytics.screen("Auth: Two Factor Screen")

        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view)

        Ui.setTextFromHtml(view, R.id.recover_auth, R.string.recover_link_text)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recover_auth.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.recovery_link))
            startActivity(intent)
        }

        action_verify.setOnClickListener { _ ->
            if (loginJob == null || loginJob?.isActive == false) {
                val code = verificationCode.text.toString()

                if (TextUtils.isEmpty(code)) {
                    verificationCode.error = getString(R.string.error_field_required)
                    verificationCode.requestFocus()
                } else {
                    arguments?.let {
                        login(it.getString(EMAIL_EXTRA), it.getString(PASSWORD_EXTRA), it.getString(MFA_EXTRA), code)
                    }
                }
            }
        }
    }

    /**
     * Attempts to sign in the account with two-factor authentication.
     */
    private fun login(email: String?, password: String?, mfaToken: String?, otp: String) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        ParticleUi.showParticleButtonProgress(view, R.id.action_verify, true)

        loginJob = launch(UI) {
            try {
                withContext(CommonPool) { sparkCloud.logIn(email!!, password!!, mfaToken!!, otp) }

                SEGAnalytics.identify(email)
                SEGAnalytics.track("Auth: Two Factor success")
                log.d("Logged in...")

                if (activity?.isFinishing == false) {
                    Navigation.findNavController(view!!).navigate(R.id.action_twoFactorFragment_to_getReadyFragment)
                }
            } catch (error: ParticleCloudException) {
                log.d("onFailed(): " + error.message)
                SEGAnalytics.track("Auth: Two Factor failure")

                if (activity?.isFinishing == false) {
                    ParticleUi.showParticleButtonProgress(view, R.id.action_verify, false)
                    verificationCode.error = error.bestMessage
                    verificationCode.requestFocus()
                }
            }
        }
    }

    companion object {
        const val EMAIL_EXTRA = "EXTRA_EMAIL"
        const val PASSWORD_EXTRA = "EXTRA_PASSWORD"
        const val MFA_EXTRA = "EXTRA_MFA_TOKEN"

        private val log = TLog.get(TwoFactorFragment::class.java)
    }
}

