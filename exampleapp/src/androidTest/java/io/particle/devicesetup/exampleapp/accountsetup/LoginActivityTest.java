package io.particle.devicesetup.exampleapp.accountsetup;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.devicesetup.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LoginActivityTest {

    @Rule
    public ActivityTestRule<LoginActivity> rule = new ActivityTestRule<>(LoginActivity.class);

    @Test
    public void onPasswordResetClicked() throws Exception {
        onView(withId(R.id.action_reset_password)).check(matches(withText(R.string.reset_password)));
    }

    @Test
    public void attemptLogin() throws Exception {
        onView(withId(R.id.action_reset_password)).check(matches(withText(R.string.reset_password)));
    }

}