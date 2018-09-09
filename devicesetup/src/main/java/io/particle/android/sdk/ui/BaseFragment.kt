package io.particle.android.sdk.ui

import android.content.Context
import android.support.annotation.RestrictTo
import android.support.v4.app.Fragment

import io.particle.android.sdk.cloud.SDKGlobals
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.utils.SEGAnalytics
import uk.co.chrisjenx.calligraphy.CalligraphyConfig
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

/**
 * Created by Julius.
 */
open class BaseFragment : Fragment() {

    override fun onAttach(newBase: Context?) {
        SEGAnalytics.initialize(newBase!!)

        if (!customFontInitDone) {
            // FIXME: make actually customizable via resources
            // (see file extension string formatting nonsense)
            CalligraphyConfig.initDefault(
                    CalligraphyConfig.Builder()
                            .setDefaultFontPath(newBase.getString(R.string.normal_text_font_name))
                            .setFontAttrId(R.attr.fontPath)
                            .build())
            customFontInitDone = true
        }
        super.onAttach(CalligraphyContextWrapper.wrap(newBase))
        SDKGlobals.init(newBase)
    }

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        var setupOnly = false
        private var customFontInitDone = false
    }
}
