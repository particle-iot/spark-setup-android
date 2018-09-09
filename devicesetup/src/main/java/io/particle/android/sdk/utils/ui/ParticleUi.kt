package io.particle.android.sdk.utils.ui

import android.support.v4.app.FragmentActivity
import android.view.View

import io.particle.android.sdk.devicesetup.R


object ParticleUi {

    // since it's specific to the SDK UI, this method assumes that the id of the
    // progress spinner in the button layout is "R.id.button_progress_indicator"
    fun showParticleButtonProgress(view: View, buttonId: Int,
                                   show: Boolean) {
        Ui.fadeViewVisibility(view, R.id.button_progress_indicator, show)
        Ui.findView<View>(view, buttonId).isEnabled = !show
    }

    fun enableBrandLogoInverseVisibilityAgainstSoftKeyboard(activity: FragmentActivity) {
        val detectingLayout: SoftKeyboardVisibilityDetectingLinearLayout = Ui.findView(activity,
                R.id.keyboard_change_detector_layout)
        detectingLayout.setOnSoftKeyboardVisibilityChangeListener(BrandImageHeaderHider(activity))
    }

    fun enableBrandLogoInverseVisibilityAgainstSoftKeyboard(view: View) {
        val detectingLayout: SoftKeyboardVisibilityDetectingLinearLayout = Ui.findView(view,
                R.id.keyboard_change_detector_layout)
        detectingLayout.setOnSoftKeyboardVisibilityChangeListener(BrandImageHeaderHider(view))
    }

    class BrandImageHeaderHider : SoftKeyboardVisibilityDetectingLinearLayout.SoftKeyboardVisibilityChangeListener {

        private val logoView: View

        constructor(activity: FragmentActivity) {
            logoView = Ui.findView(activity, R.id.brand_image_header)
        }

        constructor(view: View) {
            logoView = Ui.findView(view, R.id.brand_image_header)
        }

        override fun onSoftKeyboardShown() {
            logoView.visibility = View.GONE
        }

        override fun onSoftKeyboardHidden() {
            logoView.visibility = View.VISIBLE
        }

    }
}
