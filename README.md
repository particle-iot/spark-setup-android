<p align="center" >
<img src="http://oi60.tinypic.com/116jd51.jpg" alt="Particle" title="Particle">
</p>

# Particle Device Setup library (beta)

The Particle Device Setup library is meant for integrating the initial setup process of Particle devices in your app.
This library will enable you to easily invoke a standalone setup wizard UI for setting up internet-connect products
powered by a Photon/P0/P1. The setup UI can be easily customized by a modifying XML file values which is made available to the user. Customization includes: look & feel, colors, fonts as well as custom brand logos. There are good defaults if you don’t set these properties, but you can override the look and feel as needed to suit the look of the rest of your app.

As you may have heard, the wireless setup process for the Photon uses very different underlying technology from the Core. Where the Core used Smart Config, the Photon uses what we call “soft AP” — the Photon advertises a Wi-Fi network, you join that network from your mobile app to exchange credentials, and then the Photon connects using the Wi-Fi credentials you supplied.

With the Device Setup library, you make one simple call from your app, for example when the user hits a “setup my device” button, and a whole series of screens then guides the user through the device setup process. When the process finishes, the user is back on the screen where she hit the “setup my device” button, and your code has been passed the ID of the device she just setup and claimed.

<!---
[![CI Status](http://img.shields.io/travis/spark/SparkSetup.svg?style=flat)](https://travis-ci.org/spark/SparkSetup)
[![Version](https://img.shields.io/cocoapods/v/Spark-Setup.svg?style=flat)](http://cocoapods.org/pods/SparkSetup)
[![License](https://img.shields.io/cocoapods/l/Spark-Setup.svg?style=flat)](http://cocoapods.org/pods/SparkSetup)
[![Platform](https://img.shields.io/cocoapods/p/Spark-Setup.svg?style=flat)](http://cocoapods.org/pods/SparkSetup)
-->

**Rebranding notice**

Spark has been recently rebranded as Particle. 
Code contains `Spark` keywords as classes prefixes. this will soon be replaced with `Particle`. This should not bother or affect your code in any way.

**Beta notice**

This library is still under development and is currently released as Beta, although tested, bugs and issues may be present, some code might require cleanups.

## Usage
(WIP)flesh this out more

### Basic 

There are two prerequisites for this library:

1. you must call `ParticleDeviceSetupLibrary.init()` in your Application.onCreate() or in the onCreate() of your first Activity.  e.g.:
(WIP) fix code snippet

```java
ParticleDeviceSetupLibrary.init(this.getApplicationContext(), ...) 
```

2. You must add the following entries to your application's AndroidManifest.xml file:
(WIP) is this still required?

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

and then in order to invoke the Device setup wizard in your app:

```java
// invoking UI code
``

### Advanced

You can get an the device ID of the successfully set-up device after setup completes by:
(WIP) complete how once code is ready

```java
// ??
```

### Customization

Customize setup look and feel by modifying values and references in the `customization.xml` file under `devicesetup -> src -> main -> res -> values`. 

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

## Installation

Particle Android Device Setup library is available through as a [JCenter repository](https://bintray.com/particle/android/devicesetup/). JCenter is the default dependency repository for Android Studio. To install the Android Cloud SDK in your project, add the following to your app module gradle file:

(WIP)finalize according to JCenter published name/URL of repo. Also - what do we do with versioning here? We don't want to update the documentation each time we update version, on the other hand we don't want to break users apps when something changes

```gradle
dependencies {
    compile 'io.particle:devicesetup:0.1.0'
}
```

make sure your _main project_ gradle file contains (that's the default):

```gradle
allprojects {
    repositories {
        jcenter()
    }
}
```

then sync and rebuild your module. 


## Requirements / limitations

- Android OS 4.1 and up 
- Android Studio 1.2 and up

## Communication

- If you **need help**, use [Our community website](http://community.spark.io)
- If you **found a bug**, _and can provide steps to reliably reproduce it_, open an issue.
- If you **have a feature request**, open an issue.
- If you **want to contribute**, submit a pull request.


## Maintainers

- Jens Knutson [Github](https://github.com/jensck/) | [Google+](https://google.com/+JensKnutson)
- Ido Kleinman [Github](https://www.github.com/idokleinman) | [Twitter](https://www.twitter.com/idokleinman)

## License

Particle Android Cloud SDK is available under the Apache License 2.0. See the LICENSE file for more info.
