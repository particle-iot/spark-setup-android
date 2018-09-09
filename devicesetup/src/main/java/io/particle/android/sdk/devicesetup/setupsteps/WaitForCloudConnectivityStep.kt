package io.particle.android.sdk.devicesetup.setupsteps


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

import io.particle.android.sdk.utils.EZ


class WaitForCloudConnectivityStep internal constructor(stepConfig: StepConfig, private val ctx: Context) : SetupStep(stepConfig) {
    override val isStepFulfilled: Boolean
        get() = checkIsApiHostAvailable()

    @Throws(SetupStepException::class)
    override fun onRunStep() {
        // Wait for just a couple seconds for a WifiFacade connection if possible, in case we
        // flip from the soft AP, to mobile data, and then to WifiFacade in rapid succession.
        EZ.threadSleep(2000)
        var reachabilityRetries = 0
        var isAPIHostReachable = checkIsApiHostAvailable()
        while (!isAPIHostReachable && reachabilityRetries <= MAX_RETRIES_REACHABILITY) {
            EZ.threadSleep(2000)
            isAPIHostReachable = checkIsApiHostAvailable()
            log.d("Checked for reachability $reachabilityRetries times")
            reachabilityRetries++
        }
        if (!isAPIHostReachable) {
            throw SetupStepException("Unable to reach API host")
        }
    }

    private fun checkIsApiHostAvailable(): Boolean {
        val cm = ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        var activeNetworkInfo: NetworkInfo? = null
        if (cm != null) {
            activeNetworkInfo = cm.activeNetworkInfo
        }
        return !(activeNetworkInfo == null || !activeNetworkInfo.isConnected)

        // FIXME: why is this commented out?  See what iOS does here now.
        //        try {
        //            cloud.getDevices();
        //        } catch (Exception e) {
        //            log.e("error checking availability: ", e);
        //            // FIXME:
        //            return false;
        //            // At this stage we're technically OK with other types of errors
        //            if (set(Kind.NETWORK, Kind.UNEXPECTED).contains(e.getKind())) {
        //                return false;
        //            }
        //        }

    }

    companion object {
        private const val MAX_RETRIES_REACHABILITY = 1
    }

}
