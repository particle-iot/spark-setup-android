package io.particle.android.sdk.utils.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Dialog
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.text.Html
import android.view.View
import android.widget.TextView

object Ui {

    fun <T : View> findView(activity: FragmentActivity, id: Int): T {
        return activity.findViewById(id)
    }

    fun <T : View> findView(enclosingView: View, id: Int): T {
        return enclosingView.findViewById(id)
    }

    fun <T : View> findView(frag: Fragment, id: Int): T {
        return frag.activity!!.findViewById(id)
    }

    fun <T : View> findView(dialog: Dialog, id: Int): T {
        return dialog.findViewById(id)
    }

    fun <T : Fragment> findFrag(fragment: Fragment?, id: Int): T? {
        return fragment?.childFragmentManager?.findFragmentById(id) as T?
    }

    fun <T : Fragment> findFrag(fragment: Fragment?, tag: String): T? {
        return fragment?.childFragmentManager?.findFragmentByTag(tag) as T?
    }

    fun setText(activity: FragmentActivity, textViewId: Int, text: CharSequence?): TextView {
        val textView = findView<TextView>(activity, textViewId)
        textView.text = text
        return textView
    }

    fun setText(view: View, textViewId: Int, text: CharSequence?): TextView {
        val textView = findView<TextView>(view, textViewId)
        textView.text = text
        return textView
    }

    fun setText(frag: Fragment, textViewId: Int, text: CharSequence?): TextView {
        val textView = findView<TextView>(frag, textViewId)
        textView.text = text
        return textView
    }

    fun getText(frag: Fragment, textViewId: Int, trim: Boolean): String {
        val textView = findView<TextView>(frag, textViewId)
        val text = textView.text.toString()
        return if (trim) text.trim { it <= ' ' } else text
    }

    fun setTextFromHtml(view: View, textViewId: Int, htmlStringId: Int): TextView {
        return setTextFromHtml(view, textViewId, view.context.getString(htmlStringId))
    }

    fun setTextFromHtml(view: View, textViewId: Int, htmlString: String): TextView {
        val tv = Ui.findView<TextView>(view, textViewId)
        tv.setText(Html.fromHtml(htmlString), TextView.BufferType.SPANNABLE)
        return tv
    }

    fun fadeViewVisibility(view: View, viewId: Int, show: Boolean) {
        // Fade-in the progress spinner.
        val shortAnimTime = view.resources.getInteger(
                android.R.integer.config_shortAnimTime)
        val progressView = Ui.findView<View>(view, viewId)
        progressView.visibility = View.VISIBLE
        progressView.animate()
                .setDuration(shortAnimTime.toLong())
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        progressView.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
    }

    // Technique taken from:
    // http://blog.danlew.net/2014/08/18/fast-android-asset-theming-with-colorfilter/
    fun getTintedDrawable(ctx: Context, @DrawableRes drawableResId: Int,
                          @ColorRes colorResId: Int): Drawable {
        val drawable = ContextCompat.getDrawable(ctx, drawableResId)
        val color = ContextCompat.getColor(ctx, colorResId)
        drawable!!.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        return drawable
    }


}
