package io.particle.android.sdk.accountsetup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
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
public class PasswordResetFragment extends BaseFragment {

    private static final TLog log = TLog.get(PasswordResetActivity.class);

    public static final String EXTRA_EMAIL = "EXTRA_EMAIL";

    @Inject protected ParticleCloud sparkCloud;
    @BindView(R2.id.email) protected EditText emailView;

    public static Intent buildIntent(Context context, String email) {
        Intent i = new Intent(context, PasswordResetActivity.class);
        if (truthy(email)) {
            i.putExtra(EXTRA_EMAIL, email);
        }
        return i;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_password_reset, container, true);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        ButterKnife.bind(this, view);

        SEGAnalytics.screen("Auth: Forgot password screen");
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(getActivity());

        Ui.findView(this, R.id.action_cancel).setOnClickListener(cancelView -> //finish()
        {
        });
        emailView.setText(getActivity().getIntent().getStringExtra(EXTRA_EMAIL));
        return view;
    }

    public void onPasswordResetClicked(View v) {
        SEGAnalytics.track("Auth: Request password reset");
        final String email = emailView.getText().toString();
        if (isEmailValid(email)) {
            performReset();
        } else {
            new AlertDialog.Builder(getContext())
                    .setTitle(getString(R.string.reset_password_dialog_title))
                    .setMessage(getString(R.string.reset_paassword_dialog_please_enter_a_valid_email))
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        emailView.requestFocus();
                    })
                    .show();
        }
    }

    private void performReset() {
        ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_reset_password, true);

        Async.executeAsync(sparkCloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException {
                sparkCloud.requestPasswordReset(emailView.getText().toString());
                return null;
            }

            @Override
            public void onTaskFinished() {
                ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_reset_password, false);
            }

            @Override
            public void onSuccess(@NonNull Void result) {
                onResetAttemptFinished("Instructions for how to reset your password will be sent " +
                        "to the provided email address.  Please check your email and continue " +
                        "according to instructions.");
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                log.d("onFailed(): " + error.getMessage());
                onResetAttemptFinished("Could not find a user with supplied email address, please " +
                        " check the address supplied or create a new user via the signup screen");
            }
        });
    }

    private void onResetAttemptFinished(String content) {
        new AlertDialog.Builder(getContext())
                .setMessage(content)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private boolean isEmailValid(String email) {
        return truthy(email) && email.contains("@");
    }

}
