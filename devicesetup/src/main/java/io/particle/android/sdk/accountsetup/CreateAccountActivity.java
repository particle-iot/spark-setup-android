package io.particle.android.sdk.accountsetup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.phrase.Phrase;

import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.ui.NextActivitySelector;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.truthy;


public class CreateAccountActivity extends BaseActivity {

    private static final TLog log = TLog.get(CreateAccountActivity.class);

    /**
     * Keep track of the login task to ensure we can cancel it if requested, ensure against
     * duplicate requests, etc.
     */
    private Async.AsyncApiWorker<SparkCloud, Void> createAccountTask = null;

    // UI references.
    private EditText emailView;
    private EditText passwordView;
    private EditText verifyPasswordView;
    private EditText activationCodeView;

    private boolean useOrganizationSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);

        Ui.setText(this, R.id.create_account_header_text,
                Phrase.from(this, R.string.create_account_header_text)
                        .put("brand_name", getString(R.string.brand_name))
                        .format()
        );

        emailView = Ui.findView(this, R.id.email);
        passwordView = Ui.findView(this, R.id.password);
        verifyPasswordView = Ui.findView(this, R.id.verify_password);

        activationCodeView = Ui.findView(this, R.id.activation_code);
        activationCodeView.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(4)
        });

        useOrganizationSignup = getResources().getBoolean(R.bool.organization);

        if (useOrganizationSignup) {
            // see if we were handed an activation code...
            Uri intentUri = getIntent().getData();
            if (intentUri != null) {
                activationCodeView.setText(
                        intentUri.getQueryParameter("activation_code"));
            }

            activationCodeView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == R.id.action_log_in || id == EditorInfo.IME_NULL) {
                        attemptCreateAccount();
                        return true;
                    }
                    return false;
                }
            });

        } else {
            activationCodeView.setVisibility(View.GONE);
        }


        Button submit = Ui.findView(this, R.id.action_create_account);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptCreateAccount();
            }
        });

        Ui.setTextFromHtml(this, R.id.already_have_an_account_text, R.string.msg_user_already_has_account)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(v.getContext(), LoginActivity.class));
                        finish();
                    }
                });

        if (getResources().getBoolean(R.bool.show_sign_up_page_fine_print)) {
            String tosUri = getString(R.string.terms_of_service_uri);
            String privacyPolicyUri = getString(R.string.privacy_policy_uri);

            String finePrintText = Phrase.from(this, R.string.msg_create_account_disclaimer)
                    .put("tos_link", tosUri)
                    .put("privacy_policy_link", privacyPolicyUri)
                    .format().toString();
            Ui.setTextFromHtml(this, R.id.fine_print, finePrintText)
                    .setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            Ui.findView(this, R.id.fine_print).setVisibility(View.GONE);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptCreateAccount() {
        if (createAccountTask != null) {
            log.wtf("Sign up being attempted again even though the sign up button isn't enabled?!");
            return;
        }

        // Reset errors.
        emailView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        final String email = emailView.getText().toString();
        final String password = passwordView.getText().toString();
        final String activationCode = activationCodeView.getText().toString();

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
            passwordView.setError("Passwords do not match.");
            verifyPasswordView.setError("Passwords do not match.");
            focusView = passwordView;
            cancel = true;
        }

        if (useOrganizationSignup) {
            // Check for a valid activation code
            if (!truthy(activationCode)) {
                activationCodeView.setError(getString(R.string.error_field_required));
                focusView = activationCodeView;
                cancel = true;
            } else if (!isActivationCodeValid(activationCode)) {
                activationCodeView.setError(getString(R.string.error_invalid_activation_code));
                focusView = activationCodeView;
                cancel = true;
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();

        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            ParticleUi.showSparkButtonProgress(this, R.id.action_create_account, true);
            final SparkCloud sparkCloud = SparkCloud.get(this);
            createAccountTask = Async.executeAsync(sparkCloud, new Async.ApiWork<SparkCloud, Void>() {
                @Override
                public Void callApi(SparkCloud sparkCloud) throws SparkCloudException {
                    if (useOrganizationSignup) {
                        sparkCloud.signUpWithOrganization(email, password, activationCode, getString(R.string.organization_name));
                    } else {
                        sparkCloud.signUpWithUser(email, password);
                    }
                    return null;
                }

                @Override
                public void onTaskFinished() {
                    createAccountTask = null;
                }

                @Override
                public void onSuccess(Void result) {
                    log.d("onAccountCreated()!");
                    if (isFinishing()) {
                        return;
                    }
                    attemptLogin(email, password);
                }

                @Override
                public void onFailure(SparkCloudException error) {
                    // FIXME: look at old Spark app for what we do here UI & workflow-wise
                    log.d("onFailed()");
                    ParticleUi.showSparkButtonProgress(CreateAccountActivity.this,
                            R.id.action_create_account, false);

                    String msg = "Unknown error";
                    if (error.getKind() == SparkCloudException.Kind.NETWORK) {
                        msg = "Error communicating with server";

                    } else if (error.getResponseData() != null) {

                        if (error.getResponseData().getHttpStatusCode() == 401
                                && getResources().getBoolean(R.bool.organization)) {
                            msg = "Make sure your user email does not already exist and that you have " +
                                    "entered the activation code correctly and that it was not already used";
                        } else {
                            msg = error.getServerErrorMsg();
                        }
                    }

                    Toaster.l(CreateAccountActivity.this, msg, Gravity.CENTER_VERTICAL);
                    emailView.requestFocus();
                }
            });
        }
    }

    private boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        // FIXME: we should probably fix this number...  just making sure
        // there are no blank passwords.
        return (password.length() > 0);
    }

    private boolean isActivationCodeValid(String activationCode) {
        return activationCode != null && activationCode.length() == 4;
    }

    private void attemptLogin(final String username, final String password) {
        final SparkCloud sparkCloud = SparkCloud.get(this);
        Async.executeAsync(sparkCloud, new Async.ApiWork<SparkCloud, Void>() {
            @Override
            public Void callApi(SparkCloud sparkCloud) throws SparkCloudException {
                sparkCloud.logIn(username, password);
                return null;
            }

            @Override
            public void onSuccess(Void result) {
                log.d("Logged in...");
                if (isFinishing()) {
                    return;
                }
                startActivity(NextActivitySelector.getNextActivityIntent(
                        CreateAccountActivity.this,
                        sparkCloud,
                        SDKGlobals.getSensitiveDataStorage(),
                        SDKGlobals.getAppDataStorage()));
                finish();
            }

            @Override
            public void onFailure(SparkCloudException error) {
                log.w("onFailed(): " + error.getMessage());
                ParticleUi.showSparkButtonProgress(CreateAccountActivity.this,
                        R.id.action_create_account, false);
                passwordView.setError(error.getBestMessage());
                passwordView.requestFocus();
            }
        });

    }
}
