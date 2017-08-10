package io.particle.devicesetup.exampleapp.accountsetup;

import android.os.Build;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.devicesetup.exampleapp.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LoginActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testSetupFlow() {
        ParticleDeviceSetupLibrary.init(rule.getActivity().getApplicationContext());
        ParticleDeviceSetupLibrary.startDeviceSetup(rule.getActivity(), MainActivity.class);
        try {
            onView(withId(R.id.action_im_ready)).check(matches(isDisplayed()));
            onView(withId(R.id.action_im_ready)).perform(click());
            onView(withText(R.string.enable_wifi)).perform(click());
        } catch (NoMatchingViewException e) {
            onView(withId(R.id.email)).perform(typeText("test user"));
            onView(withId(R.id.password)).perform(typeText("test password"));
            closeSoftKeyboard();
            onView(withId(R.id.action_log_in)).perform(click());
        }
    }

    public String getNonHtmlString(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(rule.getActivity().getString(resId), Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(rule.getActivity().getString(resId)).toString();
        }
    }

}