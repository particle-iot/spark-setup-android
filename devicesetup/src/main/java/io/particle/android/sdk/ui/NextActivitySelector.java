package io.particle.android.sdk.ui;

import android.content.Context;
import android.content.Intent;

import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.devicesetup.ui.GetReadyActivity;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.persistance.AppDataStorage;
import io.particle.android.sdk.persistance.SensitiveDataStorage;
import io.particle.android.sdk.utils.TLog;

import static io.particle.android.sdk.utils.Py.truthy;

/**
 * Selects the next Activity in the workflow, up to the "GetReady" screen or main UI.
 */
public class NextActivitySelector {

    private static final TLog log = TLog.get(NextActivitySelector.class);


    private final Context ctx;
    private final SparkCloud cloud;
    private final SensitiveDataStorage credStorage;
    private final AppDataStorage appData;
    private final ParticleDeviceSetupLibrary.IntentBuilder intentBuilder;

    private NextActivitySelector(Context ctx, SparkCloud cloud,
                                 SensitiveDataStorage credStorage,
                                 AppDataStorage appData,
                                 ParticleDeviceSetupLibrary.IntentBuilder intentBuilder) {
        this.ctx = ctx;
        this.cloud = cloud;
        this.credStorage = credStorage;
        this.appData = appData;
        this.intentBuilder = intentBuilder;
    }

    public static Intent getNextActivityIntent(Context ctx, SparkCloud sparkCloud,
                                               SensitiveDataStorage credStorage,
                                               AppDataStorage appDataStorage) {
        NextActivitySelector selector = new NextActivitySelector(ctx, sparkCloud, credStorage,
                appDataStorage, ParticleDeviceSetupLibrary.getInstance().getIntentBuilder());
        return selector.buildIntentForNextActivity();
    }

    Intent buildIntentForNextActivity() {
        if (!hasUserBeenLoggedInBefore()) {
            log.d("User has not been logged in before");
            return intentBuilder.buildSignUpActivityIntent(ctx);
        }

        if (!isOAuthTokenPresent()) {
            log.d("No auth token present");
            return intentBuilder.buildLoginActivityIntent(ctx);
        }

        if (!userAccountHasDevicesClaimed()) {
            log.d("User has no devices claimed");
            // FIXME: make customizable/generic somehow?
            return new Intent(ctx, GetReadyActivity.class);

        } else {
            log.d("Returning default activity");
            return intentBuilder.buildMainActivityIntent(ctx);
        }
    }

    boolean hasUserBeenLoggedInBefore() {
        return truthy(credStorage.getUser());
    }

    boolean isOAuthTokenPresent() {
        return truthy(cloud.getAccessToken());
    }

    boolean userAccountHasDevicesClaimed() {
        return this.appData.getUserHasClaimedDevices();
    }

}
