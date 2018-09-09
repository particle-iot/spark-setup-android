package io.particle.android.sdk.devicesetup

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.annotation.RestrictTo
import android.support.annotation.VisibleForTesting
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager

import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.di.ApplicationComponent
import io.particle.android.sdk.di.ApplicationModule
import io.particle.android.sdk.di.DaggerApplicationComponent
import io.particle.android.sdk.persistance.SensitiveDataStorage
import io.particle.android.sdk.ui.BaseActivity
import io.particle.android.sdk.ui.BaseFragment
import io.particle.android.sdk.utils.Preconditions

import io.particle.android.sdk.utils.Py.any

class ParticleDeviceSetupLibrary private constructor() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    lateinit var applicationComponent: ApplicationComponent

    /**
     * The contract for the broadcast sent upon device setup completion.
     *
     *
     * *NOTE: this broadcast will be sent via the LocalBroadcastManager*
     *
     *
     * The DeviceSetupCompleteReceiver class, which wraps up this logic, has been provided as a
     * convenience.
     */
    interface DeviceSetupCompleteContract {
        companion object {

            /**
             * The BroadcastIntent action sent when the device setup process is complete.
             */
            const val ACTION_DEVICE_SETUP_COMPLETE = "ACTION_DEVICE_SETUP_COMPLETE"

            /**
             * A boolean extra indicating if the setup was successful
             */
            const val EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL = "EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL"

            /**
             * A long extra indicating the device ID of the configured device.
             *
             *
             * Value is undefined if EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL is false.
             */
            const val EXTRA_CONFIGURED_DEVICE_ID = "EXTRA_CONFIGURED_DEVICE_ID"
        }
    }


    /**
     * A convenience class which wraps DeviceSetupCompleteContract.
     *
     *
     * Just extend this and override onSetupSuccess() and onSetupFailure() to receive
     * the success/failure status.
     */
    abstract class DeviceSetupCompleteReceiver : BroadcastReceiver() {

        abstract fun onSetupSuccess(configuredDeviceId: String)

        // FIXME: add some extra error information in onSetupFailed()
        abstract fun onSetupFailure()


        /**
         * Optional convenience method for registering this receiver.
         */
        fun register(ctx: Context) {
            LocalBroadcastManager.getInstance(ctx).registerReceiver(this, buildIntentFilter())
        }

        /**
         * Optional convenience method for registering this receiver.
         */
        fun unregister(ctx: Context) {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(this)
        }

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(
                    DeviceSetupCompleteContract.EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL, false)
            val deviceId = intent.getStringExtra(DeviceSetupCompleteContract.EXTRA_CONFIGURED_DEVICE_ID)
            if (success && deviceId != null) {
                onSetupSuccess(deviceId)
            } else {
                onSetupFailure()
            }
        }

        fun buildIntentFilter(): IntentFilter {
            return IntentFilter(DeviceSetupCompleteContract.ACTION_DEVICE_SETUP_COMPLETE)
        }
    }

    @VisibleForTesting
    fun setComponent(applicationComponent: ApplicationComponent) {
        this.applicationComponent = applicationComponent
    }

    companion object {

        /**
         * Starts particle device setup.
         *
         * @param ctx Context to start setup from.
         */
        fun startDeviceSetup(ctx: Context) {
            ctx.startActivity(Intent(ctx, BaseActivity::class.java))
        }

        /**
         * Starts particle device setup. Returns results to 'onActivityResult' on a calling activity.
         *
         * @param activity    Activity to which result will be returned.
         * @param requestCode Request code for results in 'onActivityResult'.
         */
        fun startDeviceSetup(activity: Activity, requestCode: Int) {
            activity.startActivityForResult(Intent(activity, BaseActivity::class.java), requestCode)
        }

        /**
         * Starts particle device setup. Returns results to 'onActivityResult' on a calling fragment.
         *
         * @param fragment    Fragment to which result will be returned.
         * @param requestCode Request code for results in 'onActivityResult'.
         */
        fun startDeviceSetup(fragment: Fragment, requestCode: Int) {
            fragment.startActivityForResult(Intent(fragment.context, BaseActivity::class.java), requestCode)
        }

        /**
         * Initialize the device setup SDK
         *
         * @param ctx any Context (the application context will be accessed from whatever is
         * passed in here, so leaks are not a concern even if you pass in an
         * Activity here)
         */
        fun init(ctx: Context) {
            if (instance == null) {
                // ensure the cloud SDK is initialized
                ParticleCloudSDK.init(ctx)
                instance = ParticleDeviceSetupLibrary()
                instance!!.setComponent(DaggerApplicationComponent
                        .builder()
                        .applicationModule(ApplicationModule(ctx.applicationContext as Application))
                        .build())
            }
        }

        /**
         * Initialize the device setup SDK for setup only (setup flow will bypass any authentication and device claiming)
         *
         * @param ctx any Context (the application context will be accessed from whatever is
         * passed in here, so leaks are not a concern even if you pass in an
         * Activity here)
         */
        fun initWithSetupOnly(ctx: Context) {
            BaseFragment.setupOnly = true
            init(ctx)
        }

        fun getInstance(): ParticleDeviceSetupLibrary {
            Preconditions.checkNotNull(instance!!,
                    "Library instance is null: did you call ParticleDeviceSetupLibrary.init()?")
            return instance as ParticleDeviceSetupLibrary
        }

        private var instance: ParticleDeviceSetupLibrary? = null

        private fun hasUserBeenLoggedInBefore(credStorage: SensitiveDataStorage): Boolean {
            return any(credStorage.user, credStorage.token)
        }
    }
}
