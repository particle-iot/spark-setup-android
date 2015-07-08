package io.particle.android.sdk.devicesetup;

import android.content.Context;
import android.content.Intent;

import com.google.common.base.Preconditions;

import io.particle.android.sdk.cloud.SDKGlobals;

public class ParticleDeviceSetupLibrary {

    public interface IntentBuilder {

        /**
         * Build an intent which will start your app's login activity
         */
        Intent buildLoginActivityIntent(Context ctx);

        /**
         * Build an intent which will start your app's signup/register activity
         */
        Intent buildSignUpActivityIntent(Context ctx);

        /**
         * Build an intent which will launch your apps main UI
         */
        Intent buildMainActivityIntent(Context ctx);
    }

    public static void init(Context ctx, IntentBuilder intentBuilder) {
        if (instance == null) {
            // ensure the cloud SDK is initialized
            SDKGlobals.init(ctx);
            instance = new ParticleDeviceSetupLibrary(intentBuilder);
        }
    }

    public static ParticleDeviceSetupLibrary getInstance() {
        Preconditions.checkNotNull(instance,
                "Library instance is null: did you call ParticleDeviceSetupLibrary.init()?");
        return instance;
    }

    public IntentBuilder getIntentBuilder() {
        return intentBuilder;
    }

    private static ParticleDeviceSetupLibrary instance;

    private final IntentBuilder intentBuilder;

    private ParticleDeviceSetupLibrary(IntentBuilder intentBuilder) {
        this.intentBuilder = intentBuilder;
    }

}
