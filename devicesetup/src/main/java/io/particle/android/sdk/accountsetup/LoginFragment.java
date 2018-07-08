package io.particle.android.sdk.accountsetup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.squareup.phrase.Phrase;

import javax.inject.Inject;

import androidx.navigation.Navigation;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseFragment;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.truthy;

/**
 * Created by Julius.
 */
public class LoginFragment extends BaseFragment {

    private static final TLog log = TLog.get(LoginFragment.class);

    /**
     * Keep track of the login task to ensure we can cancel it if requested, ensure against
     * duplicate requests, etc.
     */
    private Async.AsyncApiWorker<ParticleCloud, Void> loginTask = null;

    // UI references.
    @BindView(R2.id.email)
    protected EditText emailView;
    @BindView(R2.id.password)
    protected EditText passwordView;

    @OnClick(R2.id.forgot_password)
    protected void onPasswordResetClicked(View view) {
        Bundle bundle = new Bundle();
        String email = emailView.getText().toString();

        if (truthy(email)) {
            bundle.putString(PasswordResetFragment.EXTRA_EMAIL, email);
        }
        Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_passwordResetFragment, bundle);
    }

    @OnEditorAction(R2.id.password)
    protected boolean onPasswordEditorAction(int id) {
        if (id == R.id.action_log_in || id == EditorInfo.IME_NULL) {
            attemptLogin();
            return true;
        }
        return false;
    }

    @OnTextChanged(value = {R2.id.email, R2.id.password}, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void afterInput() {
        emailView.setError(null);
        passwordView.setError(null);
    }

    @Inject
    protected ParticleCloud sparkCloud;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.particle_activity_login, container, false);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        ButterKnife.bind(this, view);
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view);
        SEGAnalytics.screen("Auth: Login Screen");

        Ui.setText(view, R.id.log_in_header_text,
                Phrase.from(getActivity(), R.string.log_in_header_text)
                        .put("brand_name", getString(R.string.brand_name))
                        .format()
        );

        Ui.findView(view, R.id.forgot_password);
        Ui.setTextFromHtml(view, R.id.user_has_no_account, R.string.msg_no_account)
                .setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_loginFragment_to_createAccountFragment));

        Ui.setTextFromHtml(view, R.id.forgot_password, R.string.msg_forgot_password);
        return view;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @OnClick(R2.id.action_log_in)
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
        ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_log_in, true);
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
                if (getActivity().isFinishing()) {
                    return;
                }

                Navigation.findNavController(getView()).navigateUp();
//                Intent intent = ParticleDeviceSetupLibrary.getInstance()
//                        .buildIntentForNextActivity(getActivity(), sparkCloud, SDKGlobals.getSensitiveDataStorage());
//                startActivity(intent);
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                log.d("onFailed(): " + error.getMessage());
                SEGAnalytics.track("Auth: Login failure");
                ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_log_in, false);
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
