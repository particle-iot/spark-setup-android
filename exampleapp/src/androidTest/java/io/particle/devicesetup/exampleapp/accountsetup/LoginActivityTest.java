package io.particle.devicesetup.exampleapp.accountsetup;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.devicesetup.exampleapp.EspressoDaggerMockRule;
import io.particle.devicesetup.exampleapp.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LoginActivityTest {

    @Rule public EspressoDaggerMockRule rule = new EspressoDaggerMockRule();

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, false, false);

    @Mock WifiFacade wifiFacade;

    @Test
    public void testSetupFlow() {
        String ssid = (InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getApplicationContext().getString(R.string.network_name_prefix) + "-") + "TestSSID";

        ScanResult scanResult = mock(ScanResult.class);
        scanResult.SSID = ssid;
        scanResult.capabilities = "WEP";

        List<ScanResult> scanResultList = new ArrayList<>();
        scanResultList.add(scanResult);
        when(wifiFacade.getScanResults()).thenReturn(scanResultList);

        WifiInfo wifiInfo = mock(WifiInfo.class);
        when(wifiInfo.getSSID()).thenReturn(ssid);
        when(wifiFacade.getCurrentlyConnectedSSID()).thenReturn(SSID.from(ssid));
        when(wifiFacade.getConnectionInfo()).thenReturn(wifiInfo);

        activityRule.launchActivity(null);
        ParticleDeviceSetupLibrary.startDeviceSetup(activityRule.getActivity(), MainActivity.class);
        try {
            onView(withId(R.id.action_im_ready)).check(matches(isDisplayed()));
            onView(withId(R.id.action_im_ready)).perform(click());
            onView(withText(R.string.enable_wifi)).perform(click());
            onView(withText(scanResult.SSID)).perform(click());
            SystemClock.sleep(15000);
        } catch (NoMatchingViewException e) {
            onView(withId(R.id.email)).perform(typeText("test user"));
            onView(withId(R.id.password)).perform(typeText("test password"));
            closeSoftKeyboard();
            onView(withId(R.id.action_log_in)).perform(click());
        }
    }

    public String getNonHtmlString(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(activityRule.getActivity().getString(resId), Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(activityRule.getActivity().getString(resId)).toString();
        }
    }
}