package io.particle.android.sdk.accountsetup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.segment.analytics.Properties;
import com.squareup.phrase.Phrase;

import androidx.navigation.Navigation;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.cloud.models.AccountInfo;
import io.particle.android.sdk.cloud.models.SignUpInfo;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.ui.BaseFragment;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.truthy;

/**
 * Created by Julius.
 */
public class CreateAccountFragment extends BaseFragment {
    private static final TLog log = TLog.get(CreateAccountFragment.class);

    /**
     * Keep track of the login task to ensure we can cancel it if requested, ensure against
     * duplicate requests, etc.
     */
    private Async.AsyncApiWorker<ParticleCloud, Void> createAccountTask = null;

    // UI references.
    @BindView(R2.id.first) protected EditText firstNameView;
    @BindView(R2.id.last) protected EditText lastNameView;
    @BindView(R2.id.company) protected EditText companyNameView;
    @BindView(R2.id.email) protected EditText emailView;
    @BindView(R2.id.password) protected EditText passwordView;
    @BindView(R2.id.verify_password) protected EditText verifyPasswordView;
    @BindView(R2.id.companyAccount) protected Switch companyChoiceView;

    @OnClick(R2.id.already_have_an_account_text)
    protected void onHasAccountClick(View view) {
        Navigation.findNavController(view).navigate(R.id.action_createAccountFragment_to_loginFragment);
    }

    private final CompoundButton.OnCheckedChangeListener onCompanyCheckedChange = (CompoundButton buttonView, boolean isChecked) -> {
        if (isChecked) {
            int backgroundDefault = ContextCompat.getColor(buttonView.getContext(),
                    R.color.register_field_background_color_enabled);
            verifyPasswordView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            companyNameView.setBackgroundColor(backgroundDefault);
            buttonView.setText(R.string.prompt_company_account_enabled);
        } else {
            verifyPasswordView.setImeOptions(EditorInfo.IME_ACTION_DONE);
            companyNameView.setBackgroundColor(ContextCompat.getColor(buttonView.getContext(),
                    R.color.register_field_background_color_disabled));
            buttonView.setText(R.string.prompt_company_account_disabled);
        }
        companyNameView.setEnabled(isChecked);
    };

    private boolean useOrganizationSignup;
    private boolean useProductionSignup;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.activity_create_account, container, false);
        ButterKnife.bind(this, view);
        SEGAnalytics.screen("Auth: Sign Up screen");
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view);

        companyChoiceView.setOnCheckedChangeListener(onCompanyCheckedChange);

        Ui.setText(view, R.id.create_account_header_text,
                Phrase.from(getActivity(), R.string.create_account_header_text)
                        .put("brand_name", getString(R.string.brand_name))
                        .format()
        );

        useOrganizationSignup = getResources().getBoolean(R.bool.organization);
        useProductionSignup = getResources().getBoolean(R.bool.productMode);

        Ui.setTextFromHtml(view, R.id.already_have_an_account_text, R.string.msg_user_already_has_account);

        if (getResources().getBoolean(R.bool.show_sign_up_page_fine_print)) {
            String tosUri = getString(R.string.terms_of_service_uri);
            String privacyPolicyUri = getString(R.string.privacy_policy_uri);

            String finePrintText = Phrase.from(getActivity(), R.string.msg_create_account_disclaimer)
                    .put("tos_link", tosUri)
                    .put("privacy_policy_link", privacyPolicyUri)
                    .format().toString();
            Ui.setTextFromHtml(view, R.id.fine_print, finePrintText)
                    .setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            Ui.findView(view, R.id.fine_print).setVisibility(View.GONE);
        }

        return view;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @OnClick(R2.id.action_create_account)
    public void attemptCreateAccount() {
        if (createAccountTask != null) {
            log.wtf("Sign up being attempted again even though the sign up button isn't enabled?!");
            return;
        }

        // Reset errors.
        emailView.setError(null);
        passwordView.setError(null);
        firstNameView.setError(null);
        lastNameView.setError(null);
        companyNameView.setError(null);

        // Store values at the time of the login attempt.
        final String email = emailView.getText().toString();
        final String password = passwordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (!truthy(email)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
            cancel = true;
        }

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            passwordView.setError(getString(R.string.error_field_required));
            focusView = passwordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        } else if (!password.equals(verifyPasswordView.getText().toString())) {
            passwordView.setError(getString(R.string.create_account_passswords_do_not_match));
            verifyPasswordView.setError(getString(R.string.create_account_passswords_do_not_match));
            focusView = passwordView;
            cancel = true;
        }
        boolean empty;
        // Check for a company account checked state
        if (companyChoiceView.isChecked()) {
            // Check for a valid company name
            empty = isFieldEmpty(companyNameView);
            cancel = empty || cancel;
            focusView = empty ? companyNameView : focusView;
        }
        // Check for a valid Last name
        empty = isFieldEmpty(lastNameView);
        cancel = empty || cancel;
        focusView = empty ? lastNameView : focusView;
        // Check for a valid First name
        empty = isFieldEmpty(firstNameView);
        cancel = empty || cancel;
        focusView = empty ? firstNameView : focusView;

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            attemptSignUp();
        }
    }

    private void attemptSignUp() {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setFirstName(firstNameView.getText().toString());
        accountInfo.setLastName(lastNameView.getText().toString());
        accountInfo.setCompanyName(companyNameView.getText().toString());
        accountInfo.setBusinessAccount(companyChoiceView.isChecked());
        // Store values at the time of the signup attempt.
        final String email = emailView.getText().toString();
        final String password = passwordView.getText().toString();
        SignUpInfo signUpInfo = new SignUpInfo(email, password, accountInfo);
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_create_account, true);
        final ParticleCloud cloud = ParticleCloudSDK.getCloud();
        createAccountTask = Async.executeAsync(cloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(@NonNull ParticleCloud particleCloud) throws ParticleCloudException {
                if (useOrganizationSignup && !useProductionSignup) {
                    throw new ParticleCloudException(new Exception("Organization is deprecated, use productMode instead."));
                } else if (useProductionSignup) {
                    int productId = getResources().getInteger(R.integer.product_id);
                    if (productId == 0) {
                        throw new ParticleCloudException(new Exception("Product id must be set when productMode is in use."));
                    }
                    particleCloud.signUpAndLogInWithCustomer(signUpInfo, productId);
                } else {
                    particleCloud.signUpWithUser(signUpInfo);
                }
                return null;
            }

            @Override
            public void onTaskFinished() {
                createAccountTask = null;
            }

            @Override
            public void onSuccess(@NonNull Void result) {
                singUpTaskSuccess(email, password, accountInfo, cloud);
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                signUpTaskFailure(error);
            }
        });
    }

    private void singUpTaskSuccess(String email, String password, AccountInfo accountInfo, ParticleCloud cloud) {
        SEGAnalytics.track("android account creation", new Properties()
                .putValue("email", email)
                .putValue("firstname", accountInfo.getFirstName())
                .putValue("lastname", accountInfo.getLastName())
                .putValue("isbusiness", accountInfo.isBusinessAccount())
                .putValue("company", accountInfo.getCompanyName()));
        log.d("onAccountCreated()!");
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (useOrganizationSignup || useProductionSignup) {
            // with org setup, we're already logged in upon successful account creation
            onLoginSuccess();
            SEGAnalytics.track("Auth: Signed Up New Customer");
        } else {
            SEGAnalytics.track("Auth: Signed Up New User");
            attemptLogin(email, password);
        }
    }

    private void signUpTaskFailure(@NonNull ParticleCloudException error) {
        // FIXME: look at old Spark app for what we do here UI & workflow-wise
        log.d("onFailed()");
        ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_create_account, false);

        String msg = getString(R.string.create_account_unknown_error);
        if (error.getKind() == ParticleCloudException.Kind.NETWORK) {
            msg = getString(R.string.create_account_error_communicating_with_server);

        } else if (error.getResponseData() != null) {

            if (error.getResponseData().getHttpStatusCode() == 401
                    && (getResources().getBoolean(R.bool.organization) ||
                    getResources().getBoolean(R.bool.productMode))) {
                msg = getString(R.string.create_account_account_already_exists_for_email_address);
            } else {
                msg = error.getServerErrorMsg();
            }
        }
        //TODO remove once sign up error code is fixed
        if (error.getCause() != null && error.getCause().getMessage().contains(emailView.getText().toString())) {
            msg = getString(R.string.create_account_account_already_exists_for_email_address);
        }

        Toaster.l(getActivity(), msg, Gravity.CENTER_VERTICAL);
        emailView.requestFocus();
    }

    private boolean isFieldEmpty(EditText formField) {
        if (TextUtils.isEmpty(formField.getText().toString())) {
            formField.setError(getString(R.string.error_field_required));
            return true;
        }
        return false;
    }

    private boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return (password.length() > 7);
    }

    private void onLoginSuccess() {
        if (truthy(ParticleCloudSDK.getCloud().getAccessToken()) && !BaseActivity.setupOnly) {
            Navigation.findNavController(getView()).navigate(R.id.action_createAccountFragment_to_loginFragment);
        }
    }

    private void attemptLogin(final String username, final String password) {
        final ParticleCloud cloud = ParticleCloudSDK.getCloud();
        Async.executeAsync(cloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(@NonNull ParticleCloud particleCloud) throws ParticleCloudException {
                particleCloud.logIn(username, password);
                return null;
            }

            @Override
            public void onSuccess(@NonNull Void result) {
                log.d("Logged in...");
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                onLoginSuccess();
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                log.w("onFailed(): " + error.getMessage());
                ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_create_account, false);
                passwordView.setError(error.getBestMessage());
                passwordView.requestFocus();
            }
        });

    }
}
