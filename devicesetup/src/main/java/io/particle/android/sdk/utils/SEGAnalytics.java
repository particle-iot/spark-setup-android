package io.particle.android.sdk.utils;

import android.content.Context;

import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;


/**
 * Created by Julius.
 */
public class SEGAnalytics {
    public static String analyticsKey = "";
    public static boolean analyticsOptOut = true;

    public static void initialize(Context context) {
        //FIXME need to find method call whether analytics are set or export this part to application class 
        try {
            Analytics.with(context);
        } catch (IllegalArgumentException exception) {
            // Create an analytics client with the given context and Segment write key. 
            if (!analyticsKey.isEmpty()) {
                Analytics analytics = new Analytics.Builder(context, analyticsKey).build();
                analytics.optOut(analyticsOptOut);
                Analytics.setSingletonInstance(analytics);
            }
        }
    }

    public static void track(Context context, String track) {
        if (!analyticsKey.isEmpty()) {
            Analytics.with(context).track(track);
        }
    }

    public static void screen(Context context, String screen) {
        if (!analyticsKey.isEmpty()) {
            Analytics.with(context).track(screen);
        }
    }

    public static void track(Context context, String track, Properties analyticProperties) {
        if (!analyticsKey.isEmpty()) {
            Analytics.with(context).track(track, analyticProperties);
        }
    }

    public static void identify(Context context, String email) {
        if (!analyticsKey.isEmpty()) {
            Analytics.with(context).identify(email);
        }
    }
}
