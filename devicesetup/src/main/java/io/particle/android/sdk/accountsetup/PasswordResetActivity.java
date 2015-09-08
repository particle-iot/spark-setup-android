package io.particle.android.sdk.accountsetup;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.truthy;


public class PasswordResetActivity extends AppCompatActivity {

    private static final TLog log = TLog.get(PasswordResetActivity.class);


    public static final String EXTRA_EMAIL = "EXTRA_EMAIL";


    private ParticleCloud sparkCloud;
    private EditText emailView;
    private Async.AsyncApiWorker<ParticleCloud, Void> resetTask = null;


    public static Intent buildIntent(Context context, String email) {
        Intent i = new Intent(context, PasswordResetActivity.class);
        if (truthy(email)) {
            i.putExtra(EXTRA_EMAIL, email);
        }
        return i;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);

        sparkCloud = ParticleCloud.get(this);

        Ui.findView(this, R.id.action_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                });
        emailView = Ui.findView(this, R.id.email);
    }

    public void onPasswordResetClicked(View v) {
        final String email = emailView.getText().toString();
        if (isEmailValid(email)) {
            performReset();

        } else {
            new MaterialDialog.Builder(this)
                    .theme(Theme.LIGHT)
                    .positiveText("OK")
                    .autoDismiss(true)
                    .title("Reset password")
                    .content("Please enter a valid email address.")
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            emailView.requestFocus();
                        }
                    })
                    .show();
        }
    }

    private void performReset() {
        ParticleUi.showParticleButtonProgress(this, R.id.action_reset_password, true);

        resetTask = Async.executeAsync(sparkCloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(ParticleCloud sparkCloud) throws ParticleCloudException {
                sparkCloud.requestPasswordReset(emailView.getText().toString());
                return null;
            }

            @Override
            public void onTaskFinished() {
                resetTask = null;
                ParticleUi.showParticleButtonProgress(PasswordResetActivity.this, R.id.action_reset_password, false);
            }

            @Override
            public void onSuccess(Void result) {
                onResetAttemptFinished("Instructions for how to reset your password will be sent " +
                        "to the provided email address.  Please check your email and continue " +
                        "according to instructions.");
            }

            @Override
            public void onFailure(ParticleCloudException error) {
                log.d("onFailed(): " + error.getMessage());
                onResetAttemptFinished("Could not find a user with supplied email address, please " +
                        " check the address supplied or create a new user via the signup screen");
            }
        });
    }

    private void onResetAttemptFinished(String content) {
        new MaterialDialog.Builder(this)
                .theme(Theme.LIGHT)
                .positiveText("OK")
                .autoDismiss(true)
                .content(content)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .show();
    }

    private boolean isEmailValid(String email) {
        return truthy(email) && email.contains("@");
    }

}
