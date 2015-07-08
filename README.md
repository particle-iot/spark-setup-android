# spark-setup-android
Spark Device Setup library for Android


# Getting started

(WIP)flesh this out more

There are two prerequisites for this library:

1. you must call `ParticleDeviceSetupLibrary.init()` in your Application.onCreate() or in the onCreate() of your first Activity.  e.g.:
```java
ParticleDeviceSetupLibrary.init(this.getApplicationContext(), new ParticleDeviceSetupLibrary.IntentBuilder() {
    @Override
    public Intent buildLoginActivityIntent(Context ctx) {
        return new Intent(ctx, LoginActivity.class);
    }

    @Override
    public Intent buildSignUpActivityIntent(Context ctx) {
        return new Intent(ctx, CreateAccountActivity.class);
    }

    @Override
    public Intent buildMainActivityIntent(Context ctx) {
        return new Intent(ctx, DeviceListActivity.class);
    }
});
```

2. You must add the following entries to your application's AndroidManifest.xml file:

```xml
<!-- All of the following are from the device setup lib, and must be present in your app's
manifest or you will not go to space today. -->
<activity
    android:name="io.particle.android.sdk.devicesetup.ui.DiscoverDeviceActivity"
    android:label="@string/title_activity_discover_device"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="stateHidden" />
<activity
    android:name="io.particle.android.sdk.devicesetup.ui.SelectNetworkActivity"
    android:label="@string/title_activity_select_network"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="stateHidden" />
<activity
    android:name="io.particle.android.sdk.devicesetup.ui.PasswordEntryActivity"
    android:label="@string/title_activity_password_entry"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="adjustResize|stateVisible" />
<activity
    android:name="io.particle.android.sdk.devicesetup.ui.ConnectingActivity"
    android:label="@string/title_activity_connecting"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="stateHidden" />
<activity
    android:name="io.particle.android.sdk.devicesetup.ui.SuccessActivity"
    android:label="@string/title_activity_success"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="stateHidden" />
<activity
    android:name="io.particle.android.sdk.utils.ui.WebViewActivity"
    android:label="@string/title_activity_web_view"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar" />
<activity
    android:name="io.particle.android.sdk.devicesetup.ui.GetReadyActivity"
    android:label="@string/title_activity_get_ready"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar" />
<activity
    android:name="io.particle.android.sdk.devicesetup.ui.ManualNetworkEntryActivity"
    android:label="@string/title_activity_manual_network_entry"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="adjustResize|stateVisible" />
```


