package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;

import com.squareup.phrase.Phrase;

import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.SetupResult;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.ui.NextActivitySelector;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.android.sdk.utils.ui.WebViewActivity;

import static io.particle.android.sdk.utils.Py.list;


public class SuccessActivity extends BaseActivity {

    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";

    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_SUCCESS_UNKNOWN_OWNERSHIP = 2;
    public static final int RESULT_FAILURE_CLAIMING = 3;
    public static final int RESULT_FAILURE_CONFIGURE = 4;
    public static final int RESULT_FAILURE_NO_DISCONNECT = 5;
    public static final int RESULT_FAILURE_LOST_CONNECTION_TO_DEVICE = 6;


    public static Intent buildIntent(Context ctx, int resultCode) {
        return new Intent(ctx, SuccessActivity.class)
                .putExtra(EXTRA_RESULT_CODE, resultCode);
    }

    private static final SparseArray<Pair<Integer, Integer>> resultCodesToStringIds;

    static {
        resultCodesToStringIds = new SparseArray<>(6);
        resultCodesToStringIds.put(RESULT_SUCCESS, Pair.create(
                R.string.setup_success_summary,
                R.string.setup_success_details));

        resultCodesToStringIds.put(RESULT_SUCCESS_UNKNOWN_OWNERSHIP, Pair.create(
                R.string.setup_success_unknown_ownership_summary,
                R.string.setup_success_unknown_ownership_details));

        resultCodesToStringIds.put(RESULT_FAILURE_CLAIMING, Pair.create(
                R.string.setup_failure_claiming_summary,
                R.string.setup_failure_claiming_details));

        resultCodesToStringIds.put(RESULT_FAILURE_CONFIGURE, Pair.create(
                R.string.setup_failure_configure_summary,
                R.string.setup_failure_configure_details));

        resultCodesToStringIds.put(RESULT_FAILURE_NO_DISCONNECT, Pair.create(
                R.string.setup_failure_no_disconnect_from_device_summary,
                R.string.setup_failure_no_disconnect_from_device_details));

        resultCodesToStringIds.put(RESULT_FAILURE_LOST_CONNECTION_TO_DEVICE, Pair.create(
                R.string.setup_failure_configure_summary,
                R.string.setup_failure_lost_connection_to_device));
    }

    private ParticleCloud particleCloud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        particleCloud = ParticleCloud.get(this);

        int resultCode = getIntent().getIntExtra(EXTRA_RESULT_CODE, -1);

        final boolean isSuccess = list(RESULT_SUCCESS, RESULT_SUCCESS_UNKNOWN_OWNERSHIP).contains(resultCode);
        if (!isSuccess) {
            ImageView image = Ui.findView(this, R.id.result_image);
            image.setImageResource(R.drawable.fail);
        }

        Pair<? extends CharSequence, CharSequence> resultStrings = buildUiStringPair(resultCode);
        Ui.setText(this, R.id.result_summary, resultStrings.first);
        Ui.setText(this, R.id.result_details, resultStrings.second);

        Ui.findView(this, R.id.action_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = NextActivitySelector.getNextActivityIntent(
                        v.getContext(),
                        particleCloud,
                        SDKGlobals.getSensitiveDataStorage(),
                        new SetupResult(isSuccess, isSuccess ? DeviceSetupState.deviceToBeSetUpId : null));

                // FIXME: we shouldn't do this in the lib.  looks like another argument for Fragments.
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                Intent result;
                result = new Intent(ParticleDeviceSetupLibrary.DeviceSetupCompleteContract.ACTION_DEVICE_SETUP_COMPLETE)
                        .putExtra(ParticleDeviceSetupLibrary.DeviceSetupCompleteContract.EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL, isSuccess);
                if (isSuccess) {
                    result.putExtra(ParticleDeviceSetupLibrary.DeviceSetupCompleteContract.EXTRA_CONFIGURED_DEVICE_ID,
                            DeviceSetupState.deviceToBeSetUpId);
                }
                LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(result);

                finish();
            }
        });

        Ui.setTextFromHtml(this, R.id.action_troubleshooting, R.string.troubleshooting)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(v.getContext().getString(R.string.troubleshooting_uri));
                        startActivity(WebViewActivity.buildIntent(v.getContext(), uri));
                    }
                });

    }

    private Pair<? extends CharSequence, CharSequence> buildUiStringPair(int resultCode) {
        Pair<Integer, Integer> stringIds = resultCodesToStringIds.get(resultCode);
        return Pair.create(getString(stringIds.first),
                Phrase.from(this, stringIds.second)
                        .put("device_name", getString(R.string.device_name))
                        .format());
    }

}
