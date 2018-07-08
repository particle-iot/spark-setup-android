package io.particle.android.sdk.devicesetup;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.di.ApplicationComponent;
import io.particle.android.sdk.di.ApplicationModule;
import io.particle.android.sdk.di.DaggerApplicationComponent;
import io.particle.android.sdk.persistance.SensitiveDataStorage;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.Preconditions;

import static io.particle.android.sdk.utils.Py.any;

public class ParticleDeviceSetupLibrary {
    private ApplicationComponent applicationComponent;

    /**
     * The contract for the broadcast sent upon device setup completion.
     * <p/>
     * <em>NOTE: this broadcast will be sent via the LocalBroadcastManager</em>
     * <p/>
     * The DeviceSetupCompleteReceiver class, which wraps up this logic, has been provided as a
     * convenience.
     */
    public interface DeviceSetupCompleteContract {

        /**
         * The BroadcastIntent action sent when the device setup process is complete.
         */
        String ACTION_DEVICE_SETUP_COMPLETE = "ACTION_DEVICE_SETUP_COMPLETE";

        /**
         * A boolean extra indicating if the setup was successful
         */
        String EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL = "EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL";

        /**
         * A long extra indicating the device ID of the configured device.
         * <p/>
         * Value is undefined if EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL is false.
         */
        String EXTRA_CONFIGURED_DEVICE_ID = "EXTRA_CONFIGURED_DEVICE_ID";
    }


    /**
     * A convenience class which wraps DeviceSetupCompleteContract.
     * <p/>
     * Just extend this and override onSetupSuccess() and onSetupFailure() to receive
     * the success/failure status.
     */
    public static abstract class DeviceSetupCompleteReceiver extends BroadcastReceiver {

        public abstract void onSetupSuccess(@NonNull String configuredDeviceId);

        // FIXME: add some extra error information in onSetupFailed()
        public abstract void onSetupFailure();


        /**
         * Optional convenience method for registering this receiver.
         */
        public void register(Context ctx) {
            LocalBroadcastManager.getInstance(ctx).registerReceiver(this, buildIntentFilter());
        }

        /**
         * Optional convenience method for registering this receiver.
         */
        public void unregister(Context ctx) {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(
                    DeviceSetupCompleteContract.EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL, false);
            String deviceId = intent.getStringExtra(DeviceSetupCompleteContract.EXTRA_CONFIGURED_DEVICE_ID);
            if (success && deviceId != null) {
                onSetupSuccess(deviceId);
            } else {
                onSetupFailure();
            }
        }

        public IntentFilter buildIntentFilter() {
            return new IntentFilter(DeviceSetupCompleteContract.ACTION_DEVICE_SETUP_COMPLETE);
        }
    }

    /**
     * Starts particle device setup.
     *
     * @param ctx Context to start setup from.
     */
    public static void startDeviceSetup(Context ctx) {
        ctx.startActivity(new Intent(ctx, BaseActivity.class));
    }

    /**
     * Starts particle device setup. Returns results to 'onActivityResult' on a calling activity.
     *
     * @param activity    Activity to which result will be returned.
     * @param requestCode Request code for results in 'onActivityResult'.
     */
    public static void startDeviceSetup(Activity activity, int requestCode) {
        activity.startActivityForResult(new Intent(activity, BaseActivity.class), requestCode);
    }

    /**
     * Starts particle device setup. Returns results to 'onActivityResult' on a calling fragment.
     *
     * @param fragment    Fragment to which result will be returned.
     * @param requestCode Request code for results in 'onActivityResult'.
     */
    public static void startDeviceSetup(Fragment fragment, int requestCode) {
        fragment.startActivityForResult(new Intent(fragment.getContext(), BaseActivity.class), requestCode);
    }

    /**
     * Initialize the device setup SDK
     *
     * @param ctx any Context (the application context will be accessed from whatever is
     *            passed in here, so leaks are not a concern even if you pass in an
     *            Activity here)
     */
    public static void init(Context ctx) {
        if (instance == null) {
            // ensure the cloud SDK is initialized
            ParticleCloudSDK.init(ctx);
            instance = new ParticleDeviceSetupLibrary();
            instance.setComponent(DaggerApplicationComponent
                    .builder()
                    .applicationModule(new ApplicationModule((Application) ctx.getApplicationContext()))
                    .build());
        }
    }

    /**
     * Initialize the device setup SDK for setup only (setup flow will bypass any authentication and device claiming)
     *
     * @param ctx any Context (the application context will be accessed from whatever is
     *            passed in here, so leaks are not a concern even if you pass in an
     *            Activity here)
     */
    public static void initWithSetupOnly(Context ctx) {
        BaseActivity.setupOnly = true;
        init(ctx);
    }

    public static ParticleDeviceSetupLibrary getInstance() {
        Preconditions.checkNotNull(instance,
                "Library instance is null: did you call ParticleDeviceSetupLibrary.init()?");
        return instance;
    }

    private static ParticleDeviceSetupLibrary instance;

    private ParticleDeviceSetupLibrary() {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ApplicationComponent getApplicationComponent() {
        return applicationComponent;
    }

    @VisibleForTesting
    public void setComponent(ApplicationComponent applicationComponent) {
        this.applicationComponent = applicationComponent;
    }

    private static boolean hasUserBeenLoggedInBefore(SensitiveDataStorage credStorage) {
        return any(credStorage.getUser(), credStorage.getToken());
    }
}
