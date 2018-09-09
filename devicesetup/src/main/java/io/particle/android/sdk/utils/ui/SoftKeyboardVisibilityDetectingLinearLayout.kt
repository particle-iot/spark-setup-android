package io.particle.android.sdk.utils.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout

/**
 * How can Android have gone this long without this ability being built-in...?
 *
 * Derived from: https://gist.github.com/mrleolink/8823150
 *
 */
class SoftKeyboardVisibilityDetectingLinearLayout : LinearLayout {


    private var isKeyboardShown: Boolean = false
    private var listener: SoftKeyboardVisibilityChangeListener? = null


    interface SoftKeyboardVisibilityChangeListener {

        fun onSoftKeyboardShown()

        fun onSoftKeyboardHidden()

    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)


    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            // Keyboard is hidden
            if (isKeyboardShown) {
                isKeyboardShown = false
                listener!!.onSoftKeyboardHidden()
            }
        }
        return super.dispatchKeyEventPreIme(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val proposedheight = View.MeasureSpec.getSize(heightMeasureSpec)
        val actualHeight = height
        if (actualHeight > proposedheight) {
            // Keyboard is shown
            if (!isKeyboardShown) {
                isKeyboardShown = true
                listener!!.onSoftKeyboardShown()
            }
        } else {
            // Keyboard is hidden <<< this doesn't work sometimes, so I don't use it
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setOnSoftKeyboardVisibilityChangeListener(listener: SoftKeyboardVisibilityChangeListener) {
        this.listener = listener
    }

}
