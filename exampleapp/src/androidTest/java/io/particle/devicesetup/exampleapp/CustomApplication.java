package io.particle.devicesetup.exampleapp;

import android.app.Application;

import io.particle.android.sdk.cloud.ParticleCloudSDK;

/**
 * Created by Julius.
 */

public class CustomApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ParticleCloudSDK.init(this);
    }
}
