<p align="center" >
<img src="http://oi60.tinypic.com/116jd51.jpg" alt="Particle" title="Particle">
</p>

# Particle Device Setup library (beta)

The Particle Device Setup library provides everything you need to offer your
users a simple initial setup process for Particle-powered devices.  This includes
all the necessary device communication code, an easily customizable UI, and a
simple developer API.

The setup UI can be easily customized by a modifying Android XML resource files.
Available customizations include: look & feel, colors, fonts, custom brand logos
and more.  Customization isn't required for a nice looking setup process,
though: good defaults are used throughout, with styling generally following
Google's Material Design guidelines.

With the Device Setup library, you only need to make one simple call from
your app, and the Particle setup process UI launches to guide the user
through the device setup process.  When that process finishes, the user is
returned to the Activity where they were left off, and a broadcast intent
is sent out with the ID of the device she just set up and claimed.

The wireless setup process for the Photon uses very different underlying
technology from the Core.  The Core used _SmartConfig_, while the Photon
uses what we call a “soft AP” mode: during setup, the Photon advertises
itself as a Wi-Fi network.  The mobile app configures the Android device to
connect to this soft AP network, and using this connection, it can provide
the Particle device with the credentials it needs for the Wi-Fi network
you want the to Photon to use.

**Rebranding notice**

Spark recently rebranded as Particle.  Some themes and code still contains `Spark` in their names.
This will soon be replaced with `Particle`, but API impact should be minimal.

**Beta notice**

This library is still under development and is currently in beta.  Although it is tested
and mostly API-stable, bugs and other issues may be present, and the API may change prior
to leaving beta.


## Getting Started

The library is available as a Gradle dependency via [JCenter](https://bintray.com/particle/android/devicesetup/).  See the Installation section below for more details.
**TL;DR**: just add `compile 'io.particle:devicesetup:0.2.0'` to your `build.gradle`. Sync, build, installed!

You can also [download the Library as a zip](https://github.com/spark/spark-setup-android/archive/master.zip).

For a basic usage example the `example_app` module included in the Android Studio project of the library.

## Usage

### Basic 

The Device Setup library has two main requirements:

- You must call `ParticleDeviceSetupLibrary.init(...)` in your Application.onCreate() or in the
onCreate() of your first Activity, e.g.:
```java
    ParticleDeviceSetupLibrary.init(this.getApplicationContext(), MyMainActivity.class);
```

The class passed in as the second argument to `init()` is used to return you to the
"main activity" of your app once setup has completed (or whatever other activity you
wish to start once setup is complete).


- You must add all of the following entries to your application's `AndroidManifest.xml` file.
(Due to Android platform requirements, we cannot provide these manifest entries for
you automatically.)

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
<activity
    android:name="io.particle.android.sdk.accountsetup.CreateAccountActivity"
    android:label="@string/title_activity_create_account"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="adjustResize|stateHidden" />
<activity
    android:name="io.particle.android.sdk.accountsetup.LoginActivity"
    android:label="@string/title_activity_login"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="adjustResize|stateHidden" />
<activity
    android:name="io.particle.android.sdk.accountsetup.PasswordResetActivity"
    android:label="@string/title_activity_password_reset"
    android:screenOrientation="portrait"
    android:theme="@style/ParticleSetupTheme.NoActionBar"
    android:windowSoftInputMode="adjustResize|stateVisible" />
```


Then, to invoke the Device Setup wizard in your app, just call:

```java
ParticleDeviceSetupLibrary.startDeviceSetup(someContext);
```


### Advanced

You can get the device ID of the successfully set-up device after setup
completes by listening for the intent broadcast defined by
ParticleDeviceSetupLibrary.DeviceSetupCompleteContract.

A convenience wrapper for this broadcast has been created as well,
`ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver`.  Just override
the required methods, then call the `.register()` before starting the
device setup wizard, and call `.unregister()` once it's done.

```java
DeviceSetupCompleteReceiver receiver = new DeviceSetupCompleteReceiver() {

    @Override
    public void onSetupSuccess(long configuredDeviceId) {
        Toaster.s(someContext, "Hooray, you set up device " + configuredDeviceId);
    }

    @Override
    public void onSetupFailure() {
        Toaster.s(someContext, "Sorry, device setup failed.  (sad trombone)");
    }
};
receiver.register(someContext);
ParticleDeviceSetupLibrary.startDeviceSetup(someContext);
```

And then when setup is complete...
```java
receiver.unregister(someContext);
```

(tl;dr: listen for `ACTION_DEVICE_SETUP_COMPLETE`, and the device ID will be
set on the `EXTRA_CONFIGURED_DEVICE_ID` value.)


### Customization

Customize setup look and feel by overriding values from the `customization.xml` file
under `devicesetup -> src -> main -> res -> values`.

#### Product/brand info:

```xml
    <string name="brand_name">Particle</string>
    <string name="app_name">@string/brand_name</string>
    <string name="device_name">Photon</string>
    <drawable name="device_image">@drawable/photon_vector</drawable>
    <drawable name="device_image_small">@drawable/photon_vector_small</drawable>
    <drawable name="brand_image_horizontal">@drawable/particle_horizontal_blue</drawable>
    <drawable name="brand_image_vertical">@drawable/particle_vertical_blue</drawable>
    <drawable name="screen_background">@drawable/trianglifybackground</drawable>
    <color name="brand_image_background_color">#641A1A1A</color>
```

#### Technical data:

```xml
    <string name="mode_button_name">Mode button</string>
    <string name="listen_mode_led_color_name">blue</string>
    <string name="network_name_prefix">@string/device_name</string>
```

#### Legal/technical info:

```xml
    <string name="terms_of_service_uri">https://www.particle.io/tos</string>
    <string name="privacy_policy_uri">https://www.particle.io/privacy</string>
    <string name="forgot_password_uri">https://www.particle.io/forgot-password</string>
    <string name="troubleshooting_uri">https://community.particle.io/t/spark-core-troubleshooting-guide-spark-team/696</string>
    <string name="terms_of_service_html_file">NOT_DEFINED</string>
    <string name="privacy_policy_html_file">NOT_DEFINED</string>
    <string name="forgot_password_html_file">NOT_DEFINED</string>
    <string name="troubleshooting_html_file">NOT_DEFINED</string>
    <bool name="show_sign_up_page_fine_print">true</bool>
```

#### Look & feel:

```xml
    <color name="page_background_color">#F2F2F2</color>
    <color name="form_field_background_color">@android:color/white</color>
    <color name="normal_text_color">@android:color/white</color>
    <color name="link_text_color">@android:color/white</color>
    <color name="link_text_bg">#19AAAAAA</color>
    <color name="error_text_color">#FE4747</color>
    <color name="element_background_color">#00BAEC</color>
    <color name="element_background_color_dark">#0083A6</color>
    <color name="element_text_color">@android:color/white</color>
    <color name="element_text_disabled_color">#E0E0E0</color>
 ```

### Organizations:
Setting the boolean resource `organization` to `true`[1] in one of your resource files) will enable organization mode, which uses different API endpoints and requires special permissions (See Particle Dashboard).
If you enable organization mode, be sure to also provide string resources for `organization_slug` and `product_slug`, using the values you created on the [Particle Dashboard](https://docs.particle.io/guide/tools-and-features/dashboard/).
To provide the `ParticleCloud` class with correct OAuth credentials for creating customers (so app users could create an account), [read the instructions here](https://docs.particle.io/reference/android/#oauth-client-configuration).
To learn how to create these credentials for your organization [read here](https://docs.particle.io/guide/how-to-build-a-product/authentication/#creating-an-oauth-client).

[1] i.e.: adding `<bool name="organization">false</bool>`


```xml
<!-- enable organization mode -->
<bool name="organization">true</bool>
<!-- organization display name -->
<string name="organization_name">Acme Wireless-Enabled Widget Company</string>
<!-- organizational name for API endpoint URL - must specify for orgMode *new* -->
<string name="organization_slug">acme_wireless_enabled_widgets</string>
<!-- enable product string for API endpoint URL - must specify for orgMode *new* -->
<string name="product_slug">acme-widget-model-123</string>
```

## Installation

The Particle Android Device Setup library is available via
[JCenter](https://bintray.com/particle/android/devicesetup/). To include it in your
project, add this to the `dependencies` section of your app module's `build.gradle`:

```gradle
dependencies {
    compile 'io.particle:devicesetup:0.2.0'
}
```


Also note that the library is hosted on JCenter, but not Maven Central.

Make sure your top-level Gradle file contains the following:

```gradle
allprojects {
    repositories {
        jcenter()
    }
}
```

## Requirements

- Android OS 4.0 (API 15) or higher
- Android Studio 1.2 or higher

## Communication

- If you **need help**, head to [our community website](http://community.particle.io), under the `Mobile` category
- If you **found a bug**, _and can provide steps to reliably reproduce it_, open an issue, label it as `bug`.
- If you **have a feature request**, open an issue, and label it with `enhancement`.
- If you **want to contribute**, submit a pull request.  Be sure to check out spark.github.io for our contribution guidelines.  You'll also need to sign our [CLA](https://docs.google.com/a/particle.io/forms/d/1_2P-vRKGUFg5bmpcKLHO_qNZWGi5HKYnfrrkd-sbZoA/viewform).

## Maintainers

- Jens Knutson [Github](https://github.com/jensck/) | [Google+](https://google.com/+JensKnutson)
- Ido Kleinman [Github](https://www.github.com/idokleinman) | [Twitter](https://www.twitter.com/idokleinman)

## License

The Particle Device Setup library is available under the Apache License 2.0.
See the `LICENSE` file for the complete text of the license.
