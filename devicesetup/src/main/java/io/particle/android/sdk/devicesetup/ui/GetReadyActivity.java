package io.particle.android.sdk.devicesetup.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.squareup.phrase.Phrase;

import java.util.Arrays;

import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.cloud.Responses.ClaimCodeResponse;
import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Async.AsyncApiWorker;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.android.sdk.utils.ui.WebViewActivity;

import static io.particle.android.sdk.utils.Py.truthy;


public class GetReadyActivity extends BaseActivity {

    private static final TLog log = TLog.get(GetReadyActivity.class);

    private SparkCloud sparkCloud;
    private SoftAPConfigRemover softAPConfigRemover;

    private AsyncApiWorker<SparkCloud, ClaimCodeResponse> claimCodeWorker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_ready);

        sparkCloud = SparkCloud.get(this);
        softAPConfigRemover = new SoftAPConfigRemover(this);
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();

        Ui.findView(this, R.id.action_im_ready).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onReadyButtonClicked();
                    }
                }
        );
        Ui.setTextFromHtml(this, R.id.action_troubleshooting, R.string.troubleshooting)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(v.getContext().getString(R.string.troubleshooting_uri));
                        startActivity(WebViewActivity.buildIntent(v.getContext(), uri));
                    }
                });

        Ui.setText(this, R.id.get_ready_text,
                Phrase.from(this, R.string.get_ready_text)
                        .put("device_name", getString(R.string.device_name))
                        .put("indicator_light_setup_color_name", getString(R.string.listen_mode_led_color_name))
                        .put("setup_button_identifier", getString(R.string.mode_button_name))
                        .format());

        Ui.setText(this, R.id.get_ready_text_title,
                Phrase.from(this, R.string.get_ready_title_text)
                        .put("device_name", getString(R.string.device_name))
                        .format());
    }

    @Override
    protected void onStart() {
        super.onStart();
        log.i(this.getClass().getSimpleName() + ".onStart()");
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();

        if (sparkCloud.getLoggedInUsername() == null) {
            startLoginActivity();
            finish();
        }

    }

    private void onReadyButtonClicked() {
        // FIXME: check here that another of these tasks isn't already running
        DeviceSetupState.reset();
        showProgress(true);
        claimCodeWorker = Async.executeAsync(sparkCloud, new Async.ApiWork<SparkCloud, ClaimCodeResponse>() {
            @Override
            public ClaimCodeResponse callApi(SparkCloud sparkCloud) throws SparkCloudException {
                return sparkCloud.generateClaimCode();
            }

            @Override
            public void onTaskFinished() {
                claimCodeWorker = null;
                showProgress(false);
            }

            @Override
            public void onSuccess(ClaimCodeResponse result) {
                log.d("Claim code generated: " + result.claimCode);

                DeviceSetupState.claimCode = result.claimCode;
                if (truthy(result.deviceIds)) {
                    DeviceSetupState.claimedDeviceIds.addAll(Arrays.asList(result.deviceIds));
                }

                if (!isFinishing()) {
                    startActivity(new Intent(GetReadyActivity.this, DiscoverDeviceActivity.class));
                }
            }

            @Override
            public void onFailure(SparkCloudException error) {
                log.d("Generating claim code failed");
                SparkCloudException.ResponseErrorData errorData = error.getResponseData();
                if (errorData != null && errorData.getHttpStatusCode() == 401) {

                    if (isFinishing()) {
                        sparkCloud.logOut();
                        startLoginActivity();
                        return;
                    }

                    String errorMsg = String.format("Sorry, you must be logged in as a %s customer.",
                            getString(R.string.brand_name));
                    new MaterialDialog.Builder(GetReadyActivity.this)
                            .theme(Theme.LIGHT)
                            .title(getString(R.string.access_denied))
                            .content(errorMsg)
                            .positiveText(getString(R.string.ok))
                            .dismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    log.i("Logging out user");
                                    sparkCloud.logOut();
                                    startLoginActivity();
                                    finish();
                                }
                            })
                            .autoDismiss(true)
                            .show();

                } else {
                    if (isFinishing()) {
                        return;
                    }

                    // FIXME: we could just check the internet connection here ourselves...
                    String errorMsg = getString(R.string.get_ready_could_not_connect_to_cloud);
                    if (error.getMessage() != null) {
                        errorMsg = errorMsg + "\n\n" + error.getMessage();
                    }
                    new MaterialDialog.Builder(GetReadyActivity.this)
                            .theme(Theme.LIGHT)
                            .title(getString(R.string.error))
                            .content(errorMsg)
                            .positiveText(getString(R.string.ok))
                            .autoDismiss(true)
                            .show();
                }
            }
        });
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    private void showProgress(boolean show) {
        ParticleUi.showSparkButtonProgress(this, R.id.action_im_ready, show);
    }

}
