package io.particle.android.sdk.devicesetup.ui;

import android.Manifest;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.phrase.Phrase;

import java.util.Arrays;

import javax.inject.Inject;

import androidx.navigation.Navigation;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.Responses;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.ui.BaseFragment;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.android.sdk.utils.ui.WebViewActivity;

import static io.particle.android.sdk.utils.Py.truthy;

public class GetReadyFragment extends BaseFragment implements PermissionsFragment.Client {

    private static final TLog log = TLog.get(GetReadyActivity.class);

    @Inject protected ParticleCloud sparkCloud;
    @Inject protected SoftAPConfigRemover softAPConfigRemover;

    private Async.AsyncApiWorker<ParticleCloud, Responses.ClaimCodeResponse> claimCodeWorker;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_get_ready, container, false);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        SEGAnalytics.screen("Device Setup: Get ready screen");
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();

        PermissionsFragment.ensureAttached(this);

        Ui.findView(view, R.id.action_im_ready).setOnClickListener(this::onReadyButtonClicked);

        Ui.setTextFromHtml(view, R.id.action_troubleshooting, R.string.troubleshooting)
                .setOnClickListener(v -> {
                    Uri uri = Uri.parse(v.getContext().getString(R.string.troubleshooting_uri));
                    startActivity(WebViewActivity.buildIntent(v.getContext(), uri));
                });

        Ui.setText(view, R.id.get_ready_text,
                Phrase.from(getActivity(), R.string.get_ready_text)
                        .put("device_name", getString(R.string.device_name))
                        .put("indicator_light_setup_color_name", getString(R.string.listen_mode_led_color_name))
                        .put("setup_button_identifier", getString(R.string.mode_button_name))
                        .format());

        Ui.setText(view, R.id.get_ready_text_title,
                Phrase.from(getActivity(), R.string.get_ready_title_text)
                        .put("device_name", getString(R.string.device_name))
                        .format());
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();

        if (sparkCloud.getAccessToken() == null && !BaseActivity.setupOnly) {
            getView().post(this::startLoginActivity);
        }
    }

    private void onReadyButtonClicked(View v) {
        if (claimCodeWorker != null && !claimCodeWorker.isCancelled()) {
            return;
        }
        DeviceSetupState.reset();
        if (BaseActivity.setupOnly) {
            moveToDeviceDiscovery();
            return;
        }
        showProgress(true);
        final Context ctx = getActivity();
        claimCodeWorker = Async.executeAsync(sparkCloud, new Async.ApiWork<ParticleCloud, Responses.ClaimCodeResponse>() {
            @Override
            public Responses.ClaimCodeResponse callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException {
                return generateClaimCode(ctx);
            }

            @Override
            public void onTaskFinished() {
                claimCodeWorker = null;
                showProgress(false);
            }

            @Override
            public void onSuccess(@NonNull Responses.ClaimCodeResponse result) {
                handleClaimCode(result);
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                onGenerateClaimCodeFail(error);
            }
        });
    }

    private void onGenerateClaimCodeFail(@NonNull ParticleCloudException error) {
        log.d("Generating claim code failed");
        ParticleCloudException.ResponseErrorData errorData = error.getResponseData();
        if (errorData != null && errorData.getHttpStatusCode() == 401) {
            onUnauthorizedError();
        } else {
            if (getActivity().isFinishing()) {
                return;
            }

            // FIXME: we could just check the internet connection here ourselves...
            String errorMsg = getString(R.string.get_ready_could_not_connect_to_cloud);
            if (error.getMessage() != null) {
                errorMsg = errorMsg + "\n\n" + error.getMessage();
            }
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error)
                    .setMessage(errorMsg)
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    private void onUnauthorizedError() {
        if (getActivity().isFinishing()) {
            sparkCloud.logOut();
            startLoginActivity();
            return;
        }

        String errorMsg = getString(R.string.get_ready_must_be_logged_in_as_customer,
                getString(R.string.brand_name));
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.access_denied)
                .setMessage(errorMsg)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    log.i("Logging out user");
                    sparkCloud.logOut();
                    startLoginActivity();
                })
                .show();
    }

    private void handleClaimCode(@NonNull Responses.ClaimCodeResponse result) {
        log.d("Claim code generated: " + result.claimCode);

        DeviceSetupState.claimCode = result.claimCode;
        if (truthy(result.deviceIds)) {
            DeviceSetupState.claimedDeviceIds.addAll(Arrays.asList(result.deviceIds));
        }

        if (getActivity().isFinishing()) {
            return;
        }

        moveToDeviceDiscovery();
    }

    private Responses.ClaimCodeResponse generateClaimCode(Context ctx) throws ParticleCloudException {
        Resources res = ctx.getResources();
        if (res.getBoolean(R.bool.organization) && !res.getBoolean(R.bool.productMode)) {
            return sparkCloud.generateClaimCodeForOrg(res.getString(R.string.organization_slug),
                    res.getString(R.string.product_slug));
        } else if (res.getBoolean(R.bool.productMode)) {
            int productId = res.getInteger(R.integer.product_id);
            if (productId == 0) {
                throw new ParticleCloudException(new Exception("Product id must be set when productMode is in use."));
            }
            return sparkCloud.generateClaimCode(productId);
        } else {
            return sparkCloud.generateClaimCode();
        }
    }

    private void startLoginActivity() {
        Navigation.findNavController(getView()).navigate(R.id.action_getReadyFragment_to_loginFragment);
    }

    private void showProgress(boolean show) {
        ParticleUi.showParticleButtonProgress(getActivity(), R.id.action_im_ready, show);
    }

    private void moveToDeviceDiscovery() {
        if (PermissionsFragment.hasPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Navigation.findNavController(getView()).navigate(R.id.action_getReadyFragment_to_discoverDeviceFragment);
        } else {
            PermissionsFragment.get(this).ensurePermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onUserAllowedPermission(String permission) {
        moveToDeviceDiscovery();
    }

    @Override
    public void onUserDeniedPermission(String permission) {
        Toaster.s(this, getString(R.string.location_permission_denied_cannot_start_setup));
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsFragment.get(this).onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
