package io.particle.android.sdk.devicesetup.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.utils.WorkerFragment
import io.particle.android.sdk.utils.ui.Ui
import java.util.*


class PermissionsFragment : Fragment(), OnRequestPermissionsResultCallback {

    interface Client {
        fun onUserAllowedPermission(permission: String)

        fun onUserDeniedPermission(permission: String)
    }

    fun ensurePermission(permission: String) {
        val dialogBuilder = AlertDialog.Builder(activity!!)
                .setCancelable(false)

        // FIXME: stop referring to these location permission-specific strings here,
        // try to retrieve them from the client
        if (ActivityCompat.checkSelfPermission(activity!!, permission) != PERMISSION_GRANTED || ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permission)) {
            dialogBuilder.setTitle(R.string.location_permission_dialog_title)
                    .setMessage(R.string.location_permission_dialog_text)
                    .setPositiveButton(R.string.got_it) { dialog, _ ->
                        dialog.dismiss()
                        requestPermission(permission)
                    }
        } else {
            // user has explicitly denied this permission to setup.
            // show a simple dialog and bail out.
            dialogBuilder.setTitle(R.string.location_permission_denied_dialog_title)
                    .setMessage(R.string.location_permission_denied_dialog_text)
                    .setPositiveButton(R.string.settings) { dialog, _ ->
                        dialog.dismiss()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val pkgName = activity!!.applicationInfo.packageName
                        intent.data = Uri.parse("package:$pkgName")
                        startActivity(intent)
                    }
                    .setNegativeButton(R.string.exit_setup) { _, _ ->
                        val client = activity as Client?
                        client!!.onUserDeniedPermission(permission)
                    }
        }

        dialogBuilder.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i(TAG, String.format("onRequestPermissionsResult(%d, %s, %s)",
                requestCode,
                Arrays.toString(permissions),
                Arrays.toString(grantResults)))
        if (requestCode != REQUEST_CODE) {
            Log.i(TAG, "Unrecognized request code: $requestCode")
        }

        // we only ever deal with one permission at a time, so we can always safely grab the first
        // member of this array.
        val permission = permissions[0]
        if (hasPermission(activity!!, permission)) {
            val client = activity as Client?
            client!!.onUserAllowedPermission(permission)
        } else {
            this.ensurePermission(permission)
        }
    }

    private fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(activity!!, arrayOf(permission), REQUEST_CODE)
    }

    companion object {
        val TAG = WorkerFragment.buildFragmentTag(PermissionsFragment::class.java)

        fun <T> ensureAttached(callbacksFragment: T): PermissionsFragment where T : Fragment, T : PermissionsFragment.Client {
            var frag: PermissionsFragment? = get(callbacksFragment)
            if (frag == null) {
                frag = PermissionsFragment()
                WorkerFragment.addFragment(callbacksFragment, frag, TAG)
            }
            return frag
        }

        operator fun <T> get(callbackFragment: T): PermissionsFragment? where T : Fragment?, T : PermissionsFragment.Client? {
            return Ui.findFrag(callbackFragment, TAG)
        }

        fun hasPermission(ctx: Context, permission: String): Boolean {
            val result = ContextCompat.checkSelfPermission(ctx, permission)
            return result == PERMISSION_GRANTED
        }

        private const val REQUEST_CODE = 128
    }

}
