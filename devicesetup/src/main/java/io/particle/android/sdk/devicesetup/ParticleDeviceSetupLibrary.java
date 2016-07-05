package io.particle.android.sdk.devicesetup;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.base.Preconditions;

import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.devicesetup.model.DeviceCustomization;
import io.particle.android.sdk.devicesetup.ui.GetReadyActivity;
import io.particle.android.sdk.utils.ParticleSetupConstants;


public class ParticleDeviceSetupLibrary {

    /**
     * The contract for the broadcast sent upon device setup completion.
     * <p/>
     * <em>NOTE: this broadcast will be sent via the LocalBroadcastManager</em>
     * <p/>
     * The DeviceSetupCompleteReceiver class, which wraps up this logic, has been provided as a
     * convenience.
     */
    public interface DeviceSetupCompleteContract {

        /** The BroadcastIntent action sent when the device setup process is complete. */
        String ACTION_DEVICE_SETUP_COMPLETE = "ACTION_DEVICE_SETUP_COMPLETE";

        /** A boolean extra indicating if the setup was successful */
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
     *
     * Just extend this and override onSetupSuccess() and onSetupFailure() to receive
     * the success/failure status.
     */
    public static abstract class DeviceSetupCompleteReceiver extends BroadcastReceiver {

        public abstract void onSetupSuccess(@NonNull String configuredDeviceId);

        // FIXME: add some extra error information in onSetupFailed()
        public abstract void onSetupFailure();


        /** Optional convenience method for registering this receiver. */
        public void register(Context ctx) {
            LocalBroadcastManager.getInstance(ctx).registerReceiver(this, buildIntentFilter());
        }

        /** Optional convenience method for registering this receiver. */
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
     * Start the device setup process.
     */
    public static void startDeviceSetup(Context ctx) {
        ctx.startActivity(new Intent(ctx, GetReadyActivity.class));
    }

    /**
     * Start the device setup process passing a device customization object
     */
    public static void startDeviceSetup(Context ctx, DeviceCustomization customization) {
        Intent intent = new Intent(ctx, GetReadyActivity.class);
        intent.putExtra(ParticleSetupConstants.CUSTOMIZATION_TAG, customization);
        ctx.startActivity(intent);
    }

    // FIXME: allow the SDK consumer to optionally pass in some kind of dynamic intent builder here
    // instead of a static class
    // FIXME: or, stop requiring an activity at all and just use a single activity for setup which
    // uses Fragments internally...
    /**
     * Initialize the device setup SDK
     *
     * @param ctx          any Context (the application context will be accessed from whatever is
     *                     passed in here, so leaks are not a concern even if you pass in an
     *                     Activity here)
     * @param mainActivity the class for your 'main" activity, i.e.: the class you want to
     *                     return to when the setup process is complete.
     */
    public static void init(Context ctx, Class<? extends Activity> mainActivity) {
        if (instance == null) {
            // ensure the cloud SDK is initialized
            ParticleCloudSDK.init(ctx);
            instance = new ParticleDeviceSetupLibrary(mainActivity);
        }
    }

    public static ParticleDeviceSetupLibrary getInstance() {
        Preconditions.checkNotNull(instance,
                "Library instance is null: did you call ParticleDeviceSetupLibrary.init()?");
        return instance;
    }

    private static ParticleDeviceSetupLibrary instance;


    public Class<? extends Activity> getMainActivityClass() {
        return mainActivity;
    }

    private final Class<? extends Activity> mainActivity;

    private ParticleDeviceSetupLibrary(Class<? extends Activity> mainActivity) {
        this.mainActivity = mainActivity;
    }

}
