package io.particle.android.sdk.devicesetup.setupsteps


import android.net.wifi.WifiConfiguration
import android.os.Handler
import io.particle.android.sdk.devicesetup.ApConnector
import io.particle.android.sdk.devicesetup.ApConnector.Client
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.WifiFacade
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class SetupStepApReconnector(private val wifiFacade: WifiFacade, private val apConnector: ApConnector,
                             private val mainThreadHandler: Handler, private val softApSSID: SSID) {
    private val config: WifiConfiguration = ApConnector.buildUnsecuredConfig(softApSSID)

    private val isConnectedToSoftAp: Boolean
        get() = softApSSID == wifiFacade.currentlyConnectedSSID

    @Throws(IOException::class)
    internal fun ensureConnectionToSoftAp() {
        if (isConnectedToSoftAp) {
            return
        }

        val latch = CountDownLatch(1)
        val gotConnected = AtomicBoolean(false)

        mainThread(Runnable {
            apConnector.connectToAP(config, object : Client {
                override fun onApConnectionSuccessful(config: WifiConfiguration) {
                    gotConnected.set(true)
                    latch.countDown()
                }

                override fun onApConnectionFailed(config: WifiConfiguration) {
                    latch.countDown()
                }
            })
        })

        // 50ms is an arbitrary number; just give the ApConnector time to do its work and allow for
        // a slight delay for overhead, etc.
        awaitCountdown(latch, ApConnector.CONNECT_TO_DEVICE_TIMEOUT_MILLIS + 50)

        // throw if it didn't work, otherwise assume success
        if (!gotConnected.get()) {
            throw IOException("ApConnector did not finish in time; could not reconnect to soft AP")
        }
    }

    private fun mainThread(runnable: Runnable): CountDownLatch {
        val latch = CountDownLatch(1)
        mainThreadHandler.post {
            runnable.run()
            latch.countDown()
        }
        return latch
    }

    private fun awaitCountdown(latch: CountDownLatch, awaitMs: Long): Boolean {
        return try {
            latch.await(awaitMs, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            false
        }

    }

}
