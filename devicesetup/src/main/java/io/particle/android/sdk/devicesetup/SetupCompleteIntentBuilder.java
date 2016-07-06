package io.particle.android.sdk.devicesetup;

import android.content.Context;
import android.content.Intent;

public interface SetupCompleteIntentBuilder {
    Intent buildIntent(Context ctx, SetupResult result);
}
