package io.particle.android.sdk.devicesetup.loaders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.model.ScanResultNetwork;
import io.particle.android.sdk.utils.BetterAsyncTaskLoader;
import io.particle.android.sdk.utils.Funcy;
import io.particle.android.sdk.utils.Funcy.Func;
import io.particle.android.sdk.utils.Funcy.Predicate;
import io.particle.android.sdk.utils.TLog;

import static io.particle.android.sdk.utils.Py.set;


public class WifiScanResultLoader extends BetterAsyncTaskLoader<Set<ScanResultNetwork>> {

    private static final TLog log = TLog.get(WifiScanResultLoader.class);


    private final WifiManager wifiManager;
    private final WifiScannedBroadcastReceiver receiver = new WifiScannedBroadcastReceiver();

    private volatile Set<ScanResultNetwork> mostRecentResult;
    private volatile int loadCount = 0;

    public WifiScanResultLoader(Context context) {
        super(context);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean hasContent() {
        return mostRecentResult != null;
    }

    @Override
    public Set<ScanResultNetwork> getLoadedContent() {
        return mostRecentResult;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        getContext().registerReceiver(receiver, receiver.buildIntentFilter());
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        getContext().unregisterReceiver(receiver);
        cancelLoad();
    }

    @Override
    public Set<ScanResultNetwork> loadInBackground() {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        log.d("Latest (unfiltered) scan results: " + scanResults);

        if (scanResults == null) {
            scanResults = Collections.emptyList();
            log.wtf("wifiManager.getScanResults() returned null??");
        }

        if (loadCount % 3 == 0) {
            wifiManager.startScan();
        }

        loadCount++;
        // filter the list, transform the matched results, then wrap those in a Set
        mostRecentResult = set(Funcy.transformList(scanResults, ssidStartsWithProductName, toWifiNetwork));

        if (mostRecentResult.isEmpty()) {
            log.i("No SSID scan results returned after filtering by product name.  " +
                    "Do you need to change the 'network_name_prefix' resource?");
        }

        return mostRecentResult;
    }


    private class WifiScannedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            log.d("Received WifiManager.SCAN_RESULTS_AVAILABLE_ACTION broadcast");
            forceLoad();
        }

        IntentFilter buildIntentFilter() {
            return new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        }
    }


    private final Predicate<ScanResult> ssidStartsWithProductName = new Predicate<ScanResult>() {

        final String softApPrefix = getPrefix();

        @Override
        public boolean test(ScanResult input) {
            return input.SSID != null && input.SSID.toLowerCase().startsWith(softApPrefix);
        }

        String getPrefix() {
            return (getContext().getString(R.string.network_name_prefix)+ "-").toLowerCase();
        }

    };


    private static final Func<ScanResult, ScanResultNetwork> toWifiNetwork =
            new Func<ScanResult, ScanResultNetwork>() {
        @Override
        public ScanResultNetwork apply(ScanResult input) {
            return new ScanResultNetwork(input);
        }
    };

}
