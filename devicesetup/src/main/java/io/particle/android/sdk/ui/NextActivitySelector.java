package io.particle.android.sdk.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import io.particle.android.sdk.accountsetup.CreateAccountActivity;
import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.persistance.SensitiveDataStorage;
import io.particle.android.sdk.utils.TLog;

import static io.particle.android.sdk.utils.Py.any;
import static io.particle.android.sdk.utils.Py.truthy;

/**
 * Selects the next Activity in the workflow, up to the "GetReady" screen or main UI.
 */
public class NextActivitySelector {

    private static final TLog log = TLog.get(NextActivitySelector.class);


    public static Intent getNextActivityIntent(Context ctx, ParticleCloud particleCloud,
                                               SensitiveDataStorage credStorage) {
        NextActivitySelector selector = new NextActivitySelector(particleCloud, credStorage,
                ParticleDeviceSetupLibrary.getInstance().getMainActivityClass());

        Class <? extends Activity> nextActivity = selector.buildIntentForNextActivity();
        return new Intent(ctx, nextActivity);
    }


    private final ParticleCloud cloud;
    private final SensitiveDataStorage credStorage;
    private final Class<? extends Activity> mainActivityClass;

    private NextActivitySelector(ParticleCloud cloud,
                                 SensitiveDataStorage credStorage,
                                 Class<? extends Activity> mainActivityClass) {
        this.cloud = cloud;
        this.credStorage = credStorage;
        this.mainActivityClass = mainActivityClass;
    }


    Class<? extends Activity> buildIntentForNextActivity() {
        if (!hasUserBeenLoggedInBefore()) {
            log.d("User has not been logged in before");
            return CreateAccountActivity.class;
        }

        if (!isOAuthTokenPresent()) {
            log.d("No auth token present");
            return LoginActivity.class;
        }

        log.d("Returning default activity");
        return mainActivityClass;
    }

    boolean hasUserBeenLoggedInBefore() {
        return any(credStorage.getUser(), credStorage.getToken());
    }

    boolean isOAuthTokenPresent() {
        return truthy(cloud.getAccessToken());
    }

}
