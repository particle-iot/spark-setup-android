package io.particle.android.sdk.accountsetup

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.EditText
import androidx.navigation.Navigation
import com.segment.analytics.Properties
import com.squareup.phrase.Phrase
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.cloud.models.AccountInfo
import io.particle.android.sdk.cloud.models.SignUpInfo
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.ui.BaseActivity
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.ParticleUi
import io.particle.android.sdk.utils.ui.Toaster
import io.particle.android.sdk.utils.ui.Ui
import kotlinx.android.synthetic.main.activity_create_account.*
import kotlinx.android.synthetic.main.activity_create_account.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

/**
 * Created by Julius.
 */
class CreateAccountFragment : BaseFragment() {

    /**
     * Keep track of the login task to ensure we can cancel it if requested, ensure against
     * duplicate requests, etc.
     */
    private var createAccountJob: Job? = null

    private val onCompanyCheckedChange = { buttonView: CompoundButton, isChecked: Boolean ->
        if (isChecked) {
            val backgroundDefault = ContextCompat.getColor(buttonView.context,
                    R.color.register_field_background_color_enabled)
            verify_password.imeOptions = EditorInfo.IME_ACTION_NEXT
            company.setBackgroundColor(backgroundDefault)
            buttonView.setText(R.string.prompt_company_account_enabled)
        } else {
            verify_password.imeOptions = EditorInfo.IME_ACTION_DONE
            company.setBackgroundColor(ContextCompat.getColor(buttonView.context,
                    R.color.register_field_background_color_disabled))
            buttonView.setText(R.string.prompt_company_account_disabled)
        }
        company.isEnabled = isChecked
    }

    private var useOrganizationSignup: Boolean = false
    private var useProductionSignup: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.activity_create_account, container, false)
        SEGAnalytics.screen("Auth: Sign Up screen")
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view)

        view.companyAccount.setOnCheckedChangeListener(onCompanyCheckedChange)

        Ui.setText(view, R.id.create_account_header_text,
                Phrase.from(activity!!, R.string.create_account_header_text)
                        .put("brand_name", getString(R.string.brand_name))
                        .format()
        )

        useOrganizationSignup = resources.getBoolean(R.bool.organization)
        useProductionSignup = resources.getBoolean(R.bool.productMode)

        Ui.setTextFromHtml(view, R.id.already_have_an_account_text, R.string.msg_user_already_has_account)

        if (resources.getBoolean(R.bool.show_sign_up_page_fine_print)) {
            val tosUri = getString(R.string.terms_of_service_uri)
            val privacyPolicyUri = getString(R.string.privacy_policy_uri)

            val finePrintText = Phrase.from(activity!!, R.string.msg_create_account_disclaimer)
                    .put("tos_link", tosUri)
                    .put("privacy_policy_link", privacyPolicyUri)
                    .format().toString()
            Ui.setTextFromHtml(view, R.id.fine_print, finePrintText).movementMethod = LinkMovementMethod.getInstance()
        } else {
            view.fine_print.visibility = View.GONE
        }

        view.already_have_an_account_text.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_createAccountFragment_to_loginFragment)
        }

        view.action_create_account.setOnClickListener {
            attemptCreateAccount()
        }

        return view
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptCreateAccount() {
        if (createAccountJob != null) {
            log.wtf("Sign up being attempted again even though the sign up button isn't enabled?!")
            return
        }

        // Reset errors.
        email.error = null
        password.error = null
        first.error = null
        last.error = null
        company.error = null

        // Store values at the time of the login attempt.
        val emailValue = email.text.toString()
        val passwordValue = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid email address.
        if (!truthy(emailValue)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailValue)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        // Check for a valid password.
        if (TextUtils.isEmpty(passwordValue)) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        } else if (!isPasswordValid(passwordValue)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        } else if (passwordValue != verify_password.text.toString()) {
            password.error = getString(R.string.create_account_passswords_do_not_match)
            verify_password.error = getString(R.string.create_account_passswords_do_not_match)
            focusView = password
            cancel = true
        }
        var empty: Boolean
        // Check for a company account checked state
        if (companyAccount.isChecked) {
            // Check for a valid company name
            empty = isFieldEmpty(company)
            cancel = empty || cancel
            focusView = if (empty) company else focusView
        }
        // Check for a valid Last name
        empty = isFieldEmpty(last)
        cancel = empty || cancel
        focusView = if (empty) last else focusView
        // Check for a valid First name
        empty = isFieldEmpty(first)
        cancel = empty || cancel
        focusView = if (empty) first else focusView

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            attemptSignUp()
        }
    }

    private fun attemptSignUp() {
        val accountInfo = AccountInfo()
        accountInfo.firstName = first.text.toString()
        accountInfo.lastName = last.text.toString()
        accountInfo.companyName = company.text.toString()
        accountInfo.isBusinessAccount = companyAccount.isChecked
        // Store values at the time of the signup attempt.
        val emailValue = email.text.toString()
        val password = password.text.toString()
        val signUpInfo = SignUpInfo(emailValue, password, accountInfo)
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        ParticleUi.showParticleButtonProgress(view, R.id.action_create_account, true)


        createAccountJob = launch(UI) {
            try {
                val cloud = ParticleCloudSDK.getCloud()

                withContext(CommonPool) {
                    if (useOrganizationSignup && !useProductionSignup) {
                        throw ParticleCloudException(Exception("Organization is deprecated, use productMode instead."))
                    } else if (useProductionSignup) {
                        val productId = resources.getInteger(R.integer.product_id)
                        if (productId == 0) {
                            throw ParticleCloudException(Exception("Product id must be set when productMode is in use."))
                        }
                        cloud.signUpAndLogInWithCustomer(signUpInfo, productId)
                    } else {
                        cloud.signUpWithUser(signUpInfo)
                    }
                }

                singUpTaskSuccess(emailValue, password, accountInfo)
            } catch (error: ParticleCloudException) {
                signUpTaskFailure(error)
            }
        }
    }

    private fun singUpTaskSuccess(email: String, password: String, accountInfo: AccountInfo) {
        SEGAnalytics.track("android account creation", Properties()
                .putValue("email", email)
                .putValue("firstname", accountInfo.firstName)
                .putValue("lastname", accountInfo.lastName)
                .putValue("isbusiness", accountInfo.isBusinessAccount)
                .putValue("company", accountInfo.companyName))
        log.d("onAccountCreated()!")
        if (activity == null || activity!!.isFinishing) {
            return
        }
        if (useOrganizationSignup || useProductionSignup) {
            // with org setup, we're already logged in upon successful account creation
            onLoginSuccess()
            SEGAnalytics.track("Auth: Signed Up New Customer")
        } else {
            SEGAnalytics.track("Auth: Signed Up New User")
            attemptLogin(email, password)
        }
    }

    private fun signUpTaskFailure(error: ParticleCloudException) {
        // FIXME: look at old Spark app for what we do here UI & workflow-wise
        log.d("onFailed()")
        ParticleUi.showParticleButtonProgress(view, R.id.action_create_account, false)

        var msg = getString(R.string.create_account_unknown_error)
        if (error.kind == ParticleCloudException.Kind.NETWORK) {
            msg = getString(R.string.create_account_error_communicating_with_server)

        } else if (error.responseData != null) {

            msg = if (error.responseData.httpStatusCode == 401 && (resources.getBoolean(R.bool.organization) || resources.getBoolean(R.bool.productMode))) {
                getString(R.string.create_account_account_already_exists_for_email_address)
            } else {
                error.serverErrorMsg
            }
        }
        //TODO remove once sign up error code is fixed
        if (error.cause != null && error.cause?.message?.contains(email.text.toString()) == true) {
            msg = getString(R.string.create_account_account_already_exists_for_email_address)
        }

        Toaster.l(activity, msg, Gravity.CENTER_VERTICAL)
        email.requestFocus()
    }

    private fun isFieldEmpty(formField: EditText): Boolean {
        if (TextUtils.isEmpty(formField.text.toString())) {
            formField.error = getString(R.string.error_field_required)
            return true
        }
        return false
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 7
    }

    private fun onLoginSuccess() {
        if (truthy(ParticleCloudSDK.getCloud().accessToken) && !BaseActivity.setupOnly) {
            Navigation.findNavController(view!!).navigate(R.id.action_createAccountFragment_to_loginFragment)
        }
    }

    private fun attemptLogin(username: String, passwordValue: String) {
        val cloud = ParticleCloudSDK.getCloud()
        Async.executeAsync(cloud, object : Async.ApiWork<ParticleCloud, Void>() {
            @Throws(ParticleCloudException::class)
            override fun callApi(particleCloud: ParticleCloud): Void? {
                particleCloud.logIn(username, passwordValue)
                return null
            }

            override fun onSuccess(result: Void) {
                log.d("Logged in...")
                if (activity == null || activity!!.isFinishing) {
                    return
                }
                onLoginSuccess()
            }

            override fun onFailure(error: ParticleCloudException) {
                log.w("onFailed(): " + error.message)
                ParticleUi.showParticleButtonProgress(view, R.id.action_create_account, false)
                password.error = error.bestMessage
                password.requestFocus()
            }
        })

    }

    companion object {
        private val log = TLog.get(CreateAccountFragment::class.java)
    }
}
