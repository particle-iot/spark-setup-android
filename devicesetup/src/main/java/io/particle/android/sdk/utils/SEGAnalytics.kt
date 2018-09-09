package io.particle.android.sdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.RestrictTo

import com.segment.analytics.Analytics
import com.segment.analytics.Properties


/**
 * Created by Julius.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SEGAnalytics {
    var analyticsKey = ""
    var analyticsOptOut = true
    lateinit var analytics: Analytics
    @SuppressLint("StaticFieldLeak")

    fun initialize(context: Context) {
        try {
            analytics = Analytics.with(context)
        } catch (exception: IllegalArgumentException) {
            if (!analyticsKey.isEmpty()) {
                val analytics = Analytics.Builder(context, analyticsKey).build()
                analytics.optOut(analyticsOptOut)
                Analytics.setSingletonInstance(analytics)
            }
        }

    }

    fun track(track: String) {
        if (!analyticsKey.isEmpty()) {
            analytics.track(track)
        }
    }

    fun screen(screen: String) {
        if (!analyticsKey.isEmpty()) {
            analytics.track(screen)
        }
    }

    fun track(track: String, analyticProperties: Properties?) {
        if (!analyticsKey.isEmpty()) {
            analytics.track(track, analyticProperties)
        }
    }

    fun identify(email: String?) {
        if (!analyticsKey.isEmpty() && !email.isNullOrEmpty()) {
            analytics.identify(email!!)
        }
    }
}
