package io.particle.android.sdk.utils.ui

import android.content.Context
import android.support.v4.app.Fragment
import android.view.View
import android.widget.Toast


object Toaster {

    fun s(ctx: Context?, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    fun l(ctx: Context?, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
    }

    fun s(ctx: Context, msg: String, gravity: Int) {
        val t = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT)
        t.setGravity(gravity, 0, 0)
        t.show()
    }

    fun l(ctx: Context, msg: String, gravity: Int) {
        val t = Toast.makeText(ctx, msg, Toast.LENGTH_LONG)
        t.setGravity(gravity, 0, 0)
        t.show()
    }

    fun s(frag: Fragment, msg: String) {
        s(frag.activity, msg)
    }

    fun l(frag: Fragment, msg: String) {
        l(frag.activity, msg)
    }

    fun s(view: View, msg: String) {
        s(view.context, msg)
    }

    fun l(view: View, msg: String) {
        l(view.context, msg)
    }
}


