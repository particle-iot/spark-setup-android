package io.particle.android.sdk.accountsetup;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import com.squareup.phrase.Phrase;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.ui.NextActivitySelector;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.truthy;


public class LoginActivity extends BaseActivity {

    private static final TLog log = TLog.get(LoginActivity.class);

    /**
     * Keep track of the login task to ensure we can cancel it if requested, ensure against
     * duplicate requests, etc.
     */
    private Async.AsyncApiWorker<ParticleCloud, Void> loginTask = null;

    // UI references.
    private EditText emailView;
    private EditText passwordView;

    private ParticleCloud sparkCloud;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.particle_activity_login);

        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);
        SEGAnalytics.screen("Auth: Login Screen");
        sparkCloud = ParticleCloudSDK.getCloud();

        // Set up the login form.
        emailView = Ui.findView(this, R.id.email);
        passwordView = Ui.findView(this, R.id.password);
        passwordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == R.id.action_log_in || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        for (EditText tv : list(emailView, passwordView)) {
            tv.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    emailView.setError(null);
                    passwordView.setError(null);
                }
            });
        }

        Button submit = Ui.findView(this, R.id.action_log_in);
        submit.setOnClickListener(view -> attemptLogin());

        Ui.setText(this, R.id.log_in_header_text,
                Phrase.from(this, R.string.log_in_header_text)
                        .put("brand_name", getString(R.string.brand_name))
                        .format()
        );

        Ui.setTextFromHtml(this, R.id.user_has_no_account, R.string.msg_no_account)
                .setOnClickListener(v -> {
                    startActivity(new Intent(v.getContext(), CreateAccountActivity.class));
                    finish();
                });

        Ui.setTextFromHtml(this, R.id.forgot_password, R.string.msg_forgot_password);
    }

    public void onPasswordResetClicked(View v) {
        Intent intent;
        intent = PasswordResetActivity.buildIntent(this, emailView.getText().toString());
        startActivity(intent);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (loginTask != null) {
            log.wtf("Login being attempted again even though the button isn't enabled?!");
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


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            cancel = true;

        } else if (!isEmailValid(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            login(email, password);
        }
    }

    /**
     * Attempts to sign in the account specified by the login form.
     */
    private void login(String email, String password) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        ParticleUi.showParticleButtonProgress(this, R.id.action_log_in, true);
        loginTask = Async.executeAsync(sparkCloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException {
                sparkCloud.logIn(email, password);
                return null;
            }

            @Override
            public void onTaskFinished() {
                loginTask = null;
            }

            @Override
            public void onSuccess(@NonNull Void result) {
                SEGAnalytics.identify(email);
                SEGAnalytics.track("Auth: Login success");
                log.d("Logged in...");
                if (isFinishing()) {
                    return;
                }
                startActivity(NextActivitySelector.getNextActivityIntent(
                        LoginActivity.this,
                        sparkCloud,
                        SDKGlobals.getSensitiveDataStorage(),
                        null));
                finish();
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                log.d("onFailed(): " + error.getMessage());
                SEGAnalytics.track("Auth: Login failure");
                ParticleUi.showParticleButtonProgress(LoginActivity.this,
                        R.id.action_log_in, false);
                // FIXME: check specifically for 401 errors
                // and set a better error message?  (Seems like
                // this works fine already...)
                passwordView.setError(error.getBestMessage());
                passwordView.requestFocus();
            }
        });
    }

    private boolean isEmailValid(String email) {
        return truthy(email) && email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return (password.length() > 0);
    }

}