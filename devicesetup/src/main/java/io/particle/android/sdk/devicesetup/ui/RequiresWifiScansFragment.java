package io.particle.android.sdk.devicesetup.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.util.Log;

import io.particle.android.sdk.ui.BaseFragment;

// FIXME: doing this via Activities feels sketchy.  Find a better way when refactoring
// to use fragments (or similar)
@SuppressLint("Registered")
public class RequiresWifiScansFragment extends BaseFragment {

    @Override
    public void onStart() {
        super.onStart();
        if (!PermissionsFragment.hasPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Log.d("RequiresWifiScans", "Location permission appears to have been revoked, finishing activity...");
        }
    }
}
