package io.particle.android.sdk.devicesetup.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.util.Pair
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.segment.analytics.Properties
import com.squareup.phrase.Phrase
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.SetupResult
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.ui.BaseActivity
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.SEGAnalytics
import io.particle.android.sdk.utils.ui.ParticleUi
import io.particle.android.sdk.utils.ui.Ui
import io.particle.android.sdk.utils.ui.WebViewActivity
import kotlinx.android.synthetic.main.activity_success.*
import kotlinx.android.synthetic.main.activity_success.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import javax.inject.Inject

class SuccessFragment : BaseFragment() {
    @Inject
    lateinit var particleCloud: ParticleCloud
    private var isSuccess = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.activity_success, container, true)

        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)

        SEGAnalytics.screen("Device Setup: Setup Result Screen")

        val resultCode = activity!!.intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        isSuccess = list(RESULT_SUCCESS, RESULT_SUCCESS_UNKNOWN_OWNERSHIP).contains(resultCode)

        if (!isSuccess) {
            val image = Ui.findView<ImageView>(this, R.id.result_image)
            image.setImageResource(R.drawable.fail)
            view.device_name.visibility = View.GONE
            val analyticProperties = Properties()

            when (resultCode) {
                RESULT_FAILURE_CLAIMING -> analyticProperties.putValue("reason", "claiming failed")
                RESULT_FAILURE_CONFIGURE -> analyticProperties.putValue("reason", "cannot configure")
                RESULT_FAILURE_NO_DISCONNECT -> analyticProperties.putValue("reason", "cannot disconnect")
                RESULT_FAILURE_LOST_CONNECTION_TO_DEVICE -> analyticProperties.putValue("reason", "lost connection")
            }
            SEGAnalytics.track("Device Setup: Failure", analyticProperties)
        } else {
            showDeviceName(particleCloud)
            SEGAnalytics.track("Device Setup: Success", if (RESULT_SUCCESS_UNKNOWN_OWNERSHIP == resultCode)
                Properties().putValue("reason", "not claimed")
            else
                null)
        }

        val resultStrings = buildUiStringPair(resultCode)
        Ui.setText(this, R.id.result_summary, resultStrings.first)
        Ui.setText(this, R.id.result_details, resultStrings.second)
        Ui.setTextFromHtml(activity, R.id.action_troubleshooting, R.string.troubleshooting)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_done.setOnClickListener {
            device_name.error = null
            if (isSuccess && !BaseActivity.setupOnly) {
                if (device_name.visibility == View.VISIBLE && device_name.text.toString().isEmpty()) {
                    device_name.error = getString(R.string.error_field_required)
                } else {
                    finishSetup(it.context, device_name.text.toString(), true)
                }
            } else {
                leaveActivity(it.context, false)
            }
        }

        action_troubleshooting.setOnClickListener {
            val uri = Uri.parse(it.context.getString(R.string.troubleshooting_uri))
            startActivity(WebViewActivity.buildIntent(it.context, uri))
        }
    }

    private fun finishSetup(context: Context, deviceName: String, isSuccess: Boolean) {
        ParticleUi.showParticleButtonProgress(view, R.id.action_done, true)

        launch(UI) {
            try {
                withContext(CommonPool) {
                    val device = particleCloud.getDevice(activity!!.intent.getStringExtra(EXTRA_DEVICE_ID))
                    setDeviceName(device, deviceName)
                }

                leaveActivity(context, isSuccess)
            } catch (ex: ParticleCloudException) {
                ParticleUi.showParticleButtonProgress(view, R.id.action_done, false)
                device_name.error = getString(R.string.device_naming_failure)
            }

        }
    }

    private fun leaveActivity(context: Context, isSuccess: Boolean) {
        val intent = Intent()
        intent.putExtra(EXTRA_SETUP_RESULT, SetupResult(isSuccess, if (isSuccess) DeviceSetupState.deviceToBeSetUpId else null))
        activity!!.setResult(Activity.RESULT_OK, intent)

        val result = Intent(ParticleDeviceSetupLibrary.DeviceSetupCompleteContract.ACTION_DEVICE_SETUP_COMPLETE)
                .putExtra(ParticleDeviceSetupLibrary.DeviceSetupCompleteContract.EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL, isSuccess)
        if (isSuccess) {
            result.putExtra(ParticleDeviceSetupLibrary.DeviceSetupCompleteContract.EXTRA_CONFIGURED_DEVICE_ID,
                    DeviceSetupState.deviceToBeSetUpId)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(result)

        activity!!.finish()
    }

    @Throws(ParticleCloudException::class)
    private fun setDeviceName(device: ParticleDevice, deviceName: String) {
        //Set new device name only if it changed
        if (device.name != null && device.name != deviceName) {
            device.name = deviceName
        }
    }

    private fun showDeviceName(cloud: ParticleCloud?) {
        Async.executeAsync(cloud!!, object : Async.ApiWork<ParticleCloud, ParticleDevice>() {
            @Throws(ParticleCloudException::class)
            override fun callApi(cloud: ParticleCloud): ParticleDevice {
                return particleCloud.getDevice(activity!!.intent.getStringExtra(EXTRA_DEVICE_ID))
            }

            override fun onSuccess(particleDevice: ParticleDevice) {
                device_name_label.visibility = View.VISIBLE
                device_name.visibility = View.VISIBLE
                device_name.setText(particleDevice.name)
            }

            override fun onFailure(e: ParticleCloudException) {
                //In case setup was successful, but we cannot retrieve device naming would be a minor issue
                device_name.visibility = View.GONE
                device_name_label.visibility = View.GONE
            }
        })
    }

    private fun buildUiStringPair(resultCode: Int): Pair<out CharSequence, CharSequence> {
        val stringIds = resultCodesToStringIds.get(resultCode)
        return Pair.create(getString(stringIds.first!!),
                Phrase.from(activity!!, stringIds.second!!)
                        .put("device_name", getString(R.string.device_name))
                        .format())
    }

    companion object {
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DEVICE_ID = "EXTRA_DEVICE_ID"
        const val EXTRA_SETUP_RESULT = "EXTRA_SETUP_REUSLT"

        const val RESULT_SUCCESS = 1
        const val RESULT_SUCCESS_UNKNOWN_OWNERSHIP = 2
        const val RESULT_FAILURE_CLAIMING = 3
        const val RESULT_FAILURE_CONFIGURE = 4
        const val RESULT_FAILURE_NO_DISCONNECT = 5
        const val RESULT_FAILURE_LOST_CONNECTION_TO_DEVICE = 6


        fun buildIntent(ctx: Context, resultCode: Int, deviceId: String): Intent {
            return Intent(ctx, SuccessFragment::class.java)
                    .putExtra(EXTRA_RESULT_CODE, resultCode)
                    .putExtra(EXTRA_DEVICE_ID, deviceId)
        }

        private val resultCodesToStringIds: SparseArray<Pair<Int, Int>> = SparseArray(6)

        init {
            resultCodesToStringIds.put(RESULT_SUCCESS, Pair.create(
                    R.string.setup_success_summary,
                    R.string.setup_success_details))

            resultCodesToStringIds.put(RESULT_SUCCESS_UNKNOWN_OWNERSHIP, Pair.create(
                    R.string.setup_success_unknown_ownership_summary,
                    R.string.setup_success_unknown_ownership_details))

            resultCodesToStringIds.put(RESULT_FAILURE_CLAIMING, Pair.create(
                    R.string.setup_failure_claiming_summary,
                    R.string.setup_failure_claiming_details))

            resultCodesToStringIds.put(RESULT_FAILURE_CONFIGURE, Pair.create(
                    R.string.setup_failure_configure_summary,
                    R.string.setup_failure_configure_details))

            resultCodesToStringIds.put(RESULT_FAILURE_NO_DISCONNECT, Pair.create(
                    R.string.setup_failure_no_disconnect_from_device_summary,
                    R.string.setup_failure_no_disconnect_from_device_details))

            resultCodesToStringIds.put(RESULT_FAILURE_LOST_CONNECTION_TO_DEVICE, Pair.create(
                    R.string.setup_failure_configure_summary,
                    R.string.setup_failure_lost_connection_to_device))
        }
    }

}
