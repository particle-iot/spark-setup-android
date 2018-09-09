package io.particle.android.sdk.devicesetup


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter


class SimpleReceiver private constructor(val ctx: Context, private val intentFilter: IntentFilter,
                                         private val receiver: LambdafiableBroadcastReceiver) : BroadcastReceiver() {

    private var registered = false

    interface LambdafiableBroadcastReceiver {
        fun onReceive(context: Context, intent: Intent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        receiver.onReceive(context, intent)
    }

    fun register() {
        if (registered) {
            return
        }
        ctx.applicationContext.registerReceiver(this, intentFilter)
        registered = true
    }

    fun unregister() {
        if (!registered) {
            return
        }
        ctx.applicationContext.unregisterReceiver(this)
        registered = false
    }

    companion object {


        fun newReceiver(ctx: Context, intentFilter: IntentFilter,
                        receiver: LambdafiableBroadcastReceiver): SimpleReceiver {
            return SimpleReceiver(ctx, intentFilter, receiver)
        }


        fun newRegisteredReceiver(ctx: Context, intentFilter: IntentFilter,
                                  receiver: LambdafiableBroadcastReceiver): SimpleReceiver {
            val sr = SimpleReceiver(ctx, intentFilter, receiver)
            sr.register()
            return sr
        }
    }

}
