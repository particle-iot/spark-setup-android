package io.particle.android.sdk.accountsetup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.squareup.phrase.Phrase;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.SDKGlobals;
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
    private Async.AsyncApiWorker<ParticleCloud, Void> createAccountTask = null;

    // UI references.
    private EditText emailView;
    private EditText passwordView;
    private EditText verifyPasswordView;

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

        useOrganizationSignup = getResources().getBoolean(R.bool.organization);

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

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();

        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            ParticleUi.showParticleButtonProgress(this, R.id.action_create_account, true);
            final ParticleCloud cloud = ParticleCloud.get(this);
            createAccountTask = Async.executeAsync(cloud, new Async.ApiWork<ParticleCloud, Void>() {
                @Override
                public Void callApi(ParticleCloud particleCloud) throws ParticleCloudException {
                    if (useOrganizationSignup) {
                        particleCloud.signUpAndLogInWithCustomer(email, password,
                                getString(R.string.organization_slug));
                    } else {
                        particleCloud.signUpWithUser(email, password);
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
                    if (useOrganizationSignup) {
                        // with org setup, we're already logged in upon successful account creation
                        onLoginSuccess(cloud);
                    } else {
                        attemptLogin(email, password);
                    }
                }

                @Override
                public void onFailure(ParticleCloudException error) {
                    // FIXME: look at old Spark app for what we do here UI & workflow-wise
                    log.d("onFailed()");
                    ParticleUi.showParticleButtonProgress(CreateAccountActivity.this,
                            R.id.action_create_account, false);

                    String msg = getString(R.string.create_account_unknown_error);
                    if (error.getKind() == ParticleCloudException.Kind.NETWORK) {
                        msg = getString(R.string.create_account_error_communicating_with_server);

                    } else if (error.getResponseData() != null) {

                        if (error.getResponseData().getHttpStatusCode() == 401
                                && getResources().getBoolean(R.bool.organization)) {
                            msg = getString(R.string.create_account_account_already_exists_for_email_address);
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

    private void onLoginSuccess(ParticleCloud cloud) {
        startActivity(NextActivitySelector.getNextActivityIntent(
                CreateAccountActivity.this,
                cloud,
                SDKGlobals.getSensitiveDataStorage(),
                null));
        finish();
    }

    private void attemptLogin(final String username, final String password) {
        final ParticleCloud cloud = ParticleCloud.get(this);
        Async.executeAsync(cloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(ParticleCloud particleCloud) throws ParticleCloudException {
                particleCloud.logIn(username, password);
                return null;
            }

            @Override
            public void onSuccess(Void result) {
                log.d("Logged in...");
                if (isFinishing()) {
                    return;
                }
                onLoginSuccess(cloud);
            }

            @Override
            public void onFailure(ParticleCloudException error) {
                log.w("onFailed(): " + error.getMessage());
                ParticleUi.showParticleButtonProgress(CreateAccountActivity.this,
                        R.id.action_create_account, false);
                passwordView.setError(error.getBestMessage());
                passwordView.requestFocus();
            }
        });

    }
}
