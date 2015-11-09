package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.Builder;
import com.afollestad.materialdialogs.MaterialDialog.ButtonCallback;
import com.afollestad.materialdialogs.Theme;

import java.util.Arrays;
import java.util.Set;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.WorkerFragment;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.set;


public class PermissionsFragment extends Fragment implements OnRequestPermissionsResultCallback {

    public interface Client {
        void onUserAllowedPermission(String permission);

        void onUserDeniedPermission(String permission);
    }


    public static final String TAG = WorkerFragment.buildFragmentTag(PermissionsFragment.class);


    public static <T extends FragmentActivity & Client> PermissionsFragment ensureAttached(
            T callbacksActivity) {
        PermissionsFragment frag = get(callbacksActivity);
        if (frag == null) {
            frag = new PermissionsFragment();
            WorkerFragment.addFragment(callbacksActivity, frag, TAG);
        }
        return frag;
    }

    public static <T extends FragmentActivity & Client> PermissionsFragment get(T callbacksActivity) {
        return Ui.findFrag(callbacksActivity, TAG);
    }


    public static boolean hasPermission(@NonNull Context ctx, @NonNull String permission) {
        int result = ContextCompat.checkSelfPermission(ctx, permission);
        return (result == PackageManager.PERMISSION_GRANTED);
    }


    public void ensurePermission(final @NonNull String permission) {
        if (!haveShownPermissionDialog(getActivity(), permission)) {
            requestPermission(permission);
            markPermissionDialogShown(getActivity(), permission);
            return;
        }

        Builder dialogBuilder = new Builder(getActivity())
                .theme(Theme.LIGHT)
                .cancelable(false)
                .autoDismiss(true);

        // FIXME: stop referring to these location permission-specific strings here,
        // try to retrieve them from the client
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {
            dialogBuilder.title(R.string.location_permission_dialog_title)
                    .content(R.string.location_permission_dialog_text)
                    .positiveText(R.string.got_it)
                    .callback(new ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            requestPermission(permission);
                        }
                    });
        } else {
            // user has explicitly denied this permission to setup.
            // show a simple dialog and bail out.
            dialogBuilder.title(R.string.location_permission_denied_dialog_title)
                    .content(R.string.location_permission_denied_dialog_text)
                    .positiveText("Settings")
                    .negativeText("Exit setup")
                    .callback(new ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            String pkgName = getActivity().getApplicationInfo().packageName;
                            intent.setData(Uri.parse("package:" + pkgName));
                            startActivity(intent);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            Client client = (Client) getActivity();
                            client.onUserDeniedPermission(permission);
                        }
                    });
        }

        dialogBuilder.show();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, String.format("onRequestPermissionsResult(%d, %s, %s)",
                requestCode,
                Arrays.toString(permissions),
                Arrays.toString(grantResults)));
        if (requestCode != REQUEST_CODE) {
            Log.i(TAG, "Unrecognized request code: " + requestCode);
        }

        // we only ever deal with one permission at a time, so we can always safely grab the first
        // member of this array.
        String permission = permissions[0];
        if (hasPermission(getActivity(), permission)) {
            Client client = (Client) getActivity();
            client.onUserAllowedPermission(permission);
        } else {
            this.ensurePermission(permission);
        }
    }

    private void requestPermission(String permission) {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{permission},
                REQUEST_CODE);
    }

    private static final int REQUEST_CODE = 128;

    private static final String PREF_BUCKET_NAME = "permissionsFragmentPrefs";
    private static final String PREF_KEY_PERMISSION_DIALOGS_SHOWN = "permissionsDialogsShown";

    private static boolean haveShownPermissionDialog(Context ctx, String permission) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_BUCKET_NAME, Context.MODE_PRIVATE);
        Set<String> permissions = set();
        permissions = prefs.getStringSet(PREF_KEY_PERMISSION_DIALOGS_SHOWN, permissions);
        return permissions.contains(permission);
    }

    private static void markPermissionDialogShown(Context ctx, String permission) {
        Set<String> permissions = set();
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_BUCKET_NAME, Context.MODE_PRIVATE);

        permissions = prefs.getStringSet(PREF_KEY_PERMISSION_DIALOGS_SHOWN, permissions);
        permissions.add(permission);

        prefs.edit().putStringSet(PREF_KEY_PERMISSION_DIALOGS_SHOWN, permissions).apply();
    }

}
