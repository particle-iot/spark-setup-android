package io.particle.android.sdk.accountsetup

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo

import com.squareup.phrase.Phrase

import javax.inject.Inject

import androidx.navigation.Navigation
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.cloud.exceptions.ParticleLoginException
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.ParticleUi
import io.particle.android.sdk.utils.ui.Ui

import io.particle.android.sdk.utils.Py.truthy
import kotlinx.android.synthetic.main.particle_activity_login.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

/**
 * Created by Julius.
 */
class LoginFragment : BaseFragment() {

    /**
     * Keep track of the login task to ensure we can cancel it if requested, ensure against
     * duplicate requests, etc.
     */
    private var loginJob: Job? = null

    @Inject
    lateinit var sparkCloud: ParticleCloud

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.particle_activity_login, container, false)
        ParticleDeviceSetupLibrary
                .getInstance()
                .applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view)
        SEGAnalytics.screen("Auth: Login Screen")

        Ui.setText(view, R.id.log_in_header_text,
                Phrase.from(activity!!, R.string.log_in_header_text)
                        .put("brand_name", getString(R.string.brand_name))
                        .format())

        Ui.findView<View>(view, R.id.forgot_password)
        Ui.setTextFromHtml(view, R.id.user_has_no_account, R.string.msg_no_account)
                .setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_loginFragment_to_createAccountFragment))

        Ui.setTextFromHtml(view, R.id.forgot_password, R.string.msg_forgot_password)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        forgot_password.setOnClickListener {
            onPasswordResetClicked(it)
        }

        action_log_in.setOnClickListener {
            attemptLogin()
        }

        password.setOnEditorActionListener { _, actionId, _ ->
            onPasswordEditorAction(actionId)
        }

        val formWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                email!!.error = null
                password!!.error = null
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        email.addTextChangedListener(formWatcher)
        password.addTextChangedListener(formWatcher)
    }

    fun onPasswordResetClicked(view: View) {
        val bundle = Bundle()
        val email = email!!.text.toString()

        if (truthy(email)) {
            bundle.putString(PasswordResetFragment.EXTRA_EMAIL, email)
        }
        Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_passwordResetFragment, bundle)
    }

    fun onPasswordEditorAction(id: Int): Boolean {
        if (id == R.id.action_log_in || id == EditorInfo.IME_NULL) {
            attemptLogin()
            return true
        }
        return false
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    fun attemptLogin() {
        if (loginJob != null) {
            log.wtf("Login being attempted again even though the button isn't enabled?!")
            return
        }

        // Reset errors.
        email!!.error = null
        password!!.error = null

        // Store values at the time of the login attempt.
        val emailRaw = email!!.text.toString()
        val passwordRaw = password!!.text.toString()

        var cancel = false
        var focusView: View? = null


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordRaw) && !isPasswordValid(passwordRaw)) {
            password!!.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailRaw)) {
            email!!.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true

        } else if (!isEmailValid(emailRaw)) {
            email!!.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            login(emailRaw, passwordRaw)
        }
    }

    /**
     * Attempts to sign in the account specified by the login form.
     */
    private fun login(email: String, passwordRaw: String) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        ParticleUi.showParticleButtonProgress(view, R.id.action_log_in, true)

        loginJob = launch(UI) {
            try {
                withContext(CommonPool) { sparkCloud.logIn(email, passwordRaw) }

                SEGAnalytics.identify(email)
                SEGAnalytics.track("Auth: Login success")
                log.d("Logged in...")

                if (activity?.isFinishing == false) {
                    Navigation.findNavController(view!!).navigateUp()
                }
            } catch (error: ParticleCloudException) {
                ParticleUi.showParticleButtonProgress(view, R.id.action_log_in, false)
                val loginException = error as ParticleLoginException

                if (loginException.mfaToken != null) {
                    val bundle = Bundle()
                    bundle.putString(TwoFactorFragment.EMAIL_EXTRA, email)
                    bundle.putString(TwoFactorFragment.PASSWORD_EXTRA, passwordRaw)
                    bundle.putString(TwoFactorFragment.MFA_EXTRA, loginException.mfaToken)

                    Navigation.findNavController(view!!)
                            .navigate(R.id.action_loginFragment_to_twoFactorFragment, bundle)

                } else {
                    log.d("onFailed(): " + error.message)
                    SEGAnalytics.track("Auth: Login failure")
                    // FIXME: check specifically for 401 errors
                    // and set a better error message?  (Seems like
                    // this works fine already...)
                    password!!.error = error.getBestMessage()
                    password!!.requestFocus()
                }
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return truthy(email) && email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotEmpty()
    }

    companion object {

        private val log = TLog.get(LoginFragment::class.java)
    }

}
