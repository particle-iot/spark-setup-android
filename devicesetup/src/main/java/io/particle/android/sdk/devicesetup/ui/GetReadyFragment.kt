package io.particle.android.sdk.devicesetup.ui

import android.Manifest
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import com.squareup.phrase.Phrase

import java.util.Arrays

import javax.inject.Inject

import androidx.navigation.Navigation
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.Responses
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseActivity
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.SoftAPConfigRemover
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.ParticleUi
import io.particle.android.sdk.utils.ui.Toaster
import io.particle.android.sdk.utils.ui.Ui
import io.particle.android.sdk.utils.ui.WebViewActivity

import io.particle.android.sdk.utils.Py.truthy
import kotlinx.android.synthetic.main.activity_get_ready.view.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI

class GetReadyFragment : BaseFragment(), PermissionsFragment.Client {

    @Inject
    lateinit var sparkCloud: ParticleCloud
    @Inject
    lateinit var softAPConfigRemover: SoftAPConfigRemover

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.activity_get_ready, container, false)

        ParticleDeviceSetupLibrary.getInstance().applicationComponent.activityComponentBuilder()
                .apModule(ApModule()).build().inject(this)
        SEGAnalytics.screen("Device Setup: Get ready screen")

        softAPConfigRemover.removeAllSoftApConfigs()
        softAPConfigRemover.reenableWifiNetworks()

        PermissionsFragment.ensureAttached<GetReadyFragment>(this)

        view.action_im_ready.setOnClickListener { this.onReadyButtonClicked(it) }

        Ui.setTextFromHtml(view, R.id.action_troubleshooting, R.string.troubleshooting)
                .setOnClickListener { v ->
                    val uri = Uri.parse(v.context.getString(R.string.troubleshooting_uri))
                    startActivity(WebViewActivity.buildIntent(v.context, uri))
                }

        view.get_ready_text.setText(Phrase.from(activity!!, R.string.get_ready_text)
                .put("device_name", getString(R.string.device_name))
                .put("indicator_light_setup_color_name", getString(R.string.listen_mode_led_color_name))
                .put("setup_button_identifier", getString(R.string.mode_button_name))
                .format())

        view.get_ready_text_title.setText(Phrase.from(activity!!, R.string.get_ready_title_text)
                .put("device_name", getString(R.string.device_name))
                .format())
        return view
    }

    override fun onStart() {
        super.onStart()
        softAPConfigRemover.removeAllSoftApConfigs()
        softAPConfigRemover.reenableWifiNetworks()

        if (sparkCloud.accessToken == null && !BaseActivity.setupOnly) {
            view?.post { this.startLoginActivity() }
        }
    }

    private fun onReadyButtonClicked(v: View) {
        DeviceSetupState.reset()

        if (BaseActivity.setupOnly) {
            moveToDeviceDiscovery()
            return
        }
        showProgress(true)
        val ctx = activity

        launch(UI) {
            try {
                var claimCode = withContext(CommonPool) { generateClaimCode(ctx!!) }

                showProgress(false)
                handleClaimCode(claimCode)
            } catch (error: ParticleCloudException) {
                onGenerateClaimCodeFail(error)
            }
        }
    }

    private fun onGenerateClaimCodeFail(error: ParticleCloudException) {
        log.d("Generating claim code failed")
        val errorData = error.responseData
        if (errorData != null && errorData.httpStatusCode == 401) {
            onUnauthorizedError()
        } else {
            if (activity!!.isFinishing) {
                return
            }

            // FIXME: we could just check the internet connection here ourselves...
            var errorMsg = getString(R.string.get_ready_could_not_connect_to_cloud)
            if (error.message != null) {
                errorMsg = errorMsg + "\n\n" + error.message
            }
            AlertDialog.Builder(context!!)
                    .setTitle(R.string.error)
                    .setMessage(errorMsg)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
        }
    }

    private fun onUnauthorizedError() {
        if (activity?.isFinishing == true) {
            sparkCloud.logOut()
            startLoginActivity()
            return
        }

        val errorMsg = getString(R.string.get_ready_must_be_logged_in_as_customer,
                getString(R.string.brand_name))
        AlertDialog.Builder(context!!)
                .setTitle(R.string.access_denied)
                .setMessage(errorMsg)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    log.i("Logging out user")
                    sparkCloud.logOut()
                    startLoginActivity()
                }
                .show()
    }

    private fun handleClaimCode(result: Responses.ClaimCodeResponse) {
        log.d("Claim code generated: " + result.claimCode)

        DeviceSetupState.claimCode = result.claimCode
        if (truthy(result.deviceIds)) {
            DeviceSetupState.claimedDeviceIds.addAll(Arrays.asList(*result.deviceIds))
        }

        if (activity?.isFinishing == true) {
            return
        }

        moveToDeviceDiscovery()
    }

    private fun generateClaimCode(ctx: Context): Responses.ClaimCodeResponse {
        val res = ctx.resources
        return if (res.getBoolean(R.bool.organization) && !res.getBoolean(R.bool.productMode)) {
            sparkCloud.generateClaimCodeForOrg(res.getString(R.string.organization_slug),
                    res.getString(R.string.product_slug))
        } else if (res.getBoolean(R.bool.productMode)) {
            val productId = res.getInteger(R.integer.product_id)
            if (productId == 0) {
                throw ParticleCloudException(Exception("Product id must be set when productMode is in use."))
            }
            sparkCloud.generateClaimCode(productId)
        } else {
            sparkCloud.generateClaimCode()
        }
    }

    private fun startLoginActivity() {
        Navigation.findNavController(view!!).navigate(R.id.action_getReadyFragment_to_loginFragment)
    }

    private fun showProgress(show: Boolean) {
        ParticleUi.showParticleButtonProgress(activity, R.id.action_im_ready, show)
    }

    private fun moveToDeviceDiscovery() {
        if (PermissionsFragment.hasPermission(activity!!, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Navigation.findNavController(view!!).navigate(R.id.action_getReadyFragment_to_discoverDeviceFragment)
        } else {
            PermissionsFragment.get<GetReadyFragment>(this).ensurePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    override fun onUserAllowedPermission(permission: String) {
        moveToDeviceDiscovery()
    }

    override fun onUserDeniedPermission(permission: String) {
        Toaster.s(this, getString(R.string.location_permission_denied_cannot_start_setup))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionsFragment.get<GetReadyFragment>(this).onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private val log = TLog.get(GetReadyFragment::class.java)
    }

}
