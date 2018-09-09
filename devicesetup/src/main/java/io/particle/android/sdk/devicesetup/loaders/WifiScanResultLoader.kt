package io.particle.android.sdk.devicesetup.loaders

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.SimpleReceiver
import io.particle.android.sdk.devicesetup.model.ScanResultNetwork
import io.particle.android.sdk.utils.BetterAsyncTaskLoader
import io.particle.android.sdk.utils.Py.set
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.WifiFacade
import java.util.*


class WifiScanResultLoader(context: Context, private val wifiFacade: WifiFacade) : BetterAsyncTaskLoader<Set<ScanResultNetwork>>(context) {
    private val receiver: SimpleReceiver
    @Volatile
    private var mostRecentResult: Set<ScanResultNetwork>? = null
    @Volatile
    private var loadCount = 0

    override val loadedContent: Set<ScanResultNetwork>?
        get() = mostRecentResult

    private val ssidStartsWithProductName: (ScanResult) -> Boolean = { input ->
        val softApPrefix = (getContext().getString(R.string.network_name_prefix) + "-").toLowerCase(Locale.ROOT)
        !truthy(input.SSID) && input.SSID.toLowerCase(Locale.ROOT).startsWith(softApPrefix)
    }

    init {
        val appCtx = context.applicationContext
        receiver = SimpleReceiver.newReceiver(
                appCtx, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                object : SimpleReceiver.LambdafiableBroadcastReceiver {
                    override fun onReceive(context: Context, intent: Intent) {
                        log.d("Received WifiManager.SCAN_RESULTS_AVAILABLE_ACTION broadcast")
                        forceLoad()
                    }
                })
    }

    override fun hasContent(): Boolean {
        return mostRecentResult != null
    }

    override fun onStartLoading() {
        super.onStartLoading()
        receiver.register()
        forceLoad()
    }

    override fun onStopLoading() {
        receiver.unregister()
        cancelLoad()
    }

    override fun loadInBackground(): Set<ScanResultNetwork>? {
        val scanResults = wifiFacade.scanResults
        log.d("Latest (unfiltered) scan results: $scanResults")

        if (loadCount % 3 == 0) {
            wifiFacade.startScan()
        }

        loadCount++
        // filter the list, transform the matched results, then wrap those in a Set
        mostRecentResult = set(scanResults.filter(ssidStartsWithProductName).map { ScanResultNetwork(it) })

        if (mostRecentResult!!.isEmpty()) {
            log.i("No SSID scan results returned after filtering by product name.  " + "Do you need to change the 'network_name_prefix' resource?")
        }

        return mostRecentResult
    }

    companion object {
        private val log = TLog.get(WifiScanResultLoader::class.java)
    }

}
