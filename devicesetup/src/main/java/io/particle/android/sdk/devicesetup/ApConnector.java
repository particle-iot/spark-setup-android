package io.particle.android.sdk.devicesetup;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WiFi;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.truthy;


public class ApConnector {

    public interface Client {

        void onApConnectionSuccessful(WifiConfiguration config);

        void onApConnectionFailed(WifiConfiguration config);

    }


    private static final TLog log = TLog.get(ApConnector.class);

    private static final IntentFilter WIFI_STATE_CHANGE_FILTER = new IntentFilter(
            WifiManager.NETWORK_STATE_CHANGED_ACTION);

    private final DecoratedClient client;
    private final WifiManager wifiManager;
    private final SimpleReceiver wifiLogger;
    private final Context appContext;
    private final SoftAPConfigRemover softAPConfigRemover;
    private final Handler mainThreadHandler;
    private final List<Runnable> setupRunnables = list();

    private SimpleReceiver wifiStateChangeListener;
    private Runnable onTimeoutRunnable;

    public ApConnector(Context appContext) {
        Context app = appContext.getApplicationContext();
        this.client = new DecoratedClient();
        this.appContext = appContext;
        this.wifiManager = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
        this.softAPConfigRemover = new SoftAPConfigRemover(appContext);
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
        this.wifiLogger = SimpleReceiver.newReceiver(
                appContext, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION),
                (context, intent) -> {
                    log.d("Received " + WifiManager.NETWORK_STATE_CHANGED_ACTION);
                    log.d("EXTRA_NETWORK_INFO: " + intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
                    // this will only be present if the new state is CONNECTED
                    WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                    log.d("WIFI_INFO: " + wifiInfo);
                });
    }

    /**
     * Connect this Android device to the specified AP.
     *
     * @param config the WifiConfiguration defining which AP to connect to
     * @param timeoutInMillis how long to wait before timing out
     *
     * @return the SSID that was connected prior to calling this method.  Will be null if
     *          there was no network connected, or if already connected to the target network.
     */
    public String connectToAP(Client client, final WifiConfiguration config, long timeoutInMillis) {
        wifiLogger.register();
        this.client.setDecoratedClient(client);

        // cancel any currently running timeout, etc
        clearState();

        final WifiInfo currentConnectionInfo = wifiManager.getConnectionInfo();
        // are we already connected to the right AP?  (this could happen on retries)
        if (isAlreadyConnectedToTargetNetwork(currentConnectionInfo, config.SSID)) {
            // we're already connected to this AP, nothing to do.
            client.onApConnectionSuccessful(config);
            return null;
        }

        scheduleTimeoutCheck(timeoutInMillis, config);
        wifiStateChangeListener = SimpleReceiver.newRegisteredReceiver(
                appContext, WIFI_STATE_CHANGE_FILTER,
                (ctx, intent) -> onWifiChangeBroadcastReceived(intent, config));
        final boolean useMoreComplexConnectionProcess = Build.VERSION.SDK_INT < 18;


        // we don't need this for its atomicity, we just need it as a 'final' reference to an
        // integer which can be shared by a couple of the Runnables below
        final AtomicInteger networkID = new AtomicInteger(-1);

        // everything below is created in Runnables and scheduled on the runloop to avoid some
        // wonkiness I ran into when trying to do every one of these steps one right after
        // the other on the same thread.

        final int alreadyConfiguredId = WiFi.getConfiguredNetworkId(config.SSID, appContext);
        if (alreadyConfiguredId != -1 && !useMoreComplexConnectionProcess) {
            // For some unexplained (and probably sad-trombone-y) reason, if the AP specified was
            // already configured and had been connected to in the past, it will often get to
            // the "CONNECTING" event, but just before firing the "CONNECTED" event, the
            // WifiManager appears to change its mind and reconnects to whatever configured and
            // available AP it feels like.
            //
            // As a remedy, we pre-emptively remove that config.  *shakes fist toward Mountain View*

            setupRunnables.add(() -> {
                if (wifiManager.removeNetwork(alreadyConfiguredId)) {
                    log.d("Removed already-configured " + config.SSID + " network successfully");
                } else {
                    log.e("Somehow failed to remove the already-configured network!?");
                    // not calling this state an actual failure, since it might succeed anyhow,
                    // and if it doesn't, the worst case is a longer wait to find that out.
                }
            });
        }

        if (alreadyConfiguredId == -1 || !useMoreComplexConnectionProcess) {
            setupRunnables.add(() -> {
                log.d("Adding network " + config.SSID);
                networkID.set(wifiManager.addNetwork(config));
                if (networkID.get() == -1) {
                    log.e("Adding network " + config.SSID + " failed.");
                    client.onApConnectionFailed(config);

                } else {
                    log.i("Added network with ID " + networkID + " successfully");
                }
            });
        }

        if (useMoreComplexConnectionProcess) {
            setupRunnables.add(() -> {
                log.d("Disconnecting from networks; reconnecting momentarily.");
                wifiManager.disconnect();
            });
        }

        setupRunnables.add(() -> {
            log.i("Enabling network " + config.SSID + " with network ID " + networkID.get());
            wifiManager.enableNetwork(networkID.get(), !useMoreComplexConnectionProcess);
        });

        if (useMoreComplexConnectionProcess) {
            setupRunnables.add(() -> {
                log.d("Disconnecting from networks; reconnecting momentarily.");
                wifiManager.reconnect();
            });
        }

        String currentlyConnectedSSID = WiFi.getCurrentlyConnectedSSID(appContext);
        softAPConfigRemover.onWifiNetworkDisabled(currentlyConnectedSSID);

        long timeout = 0;
        for (Runnable runnable : setupRunnables) {
            EZ.runOnMainThreadDelayed(timeout, runnable);
            timeout += 1500;
        }

        return currentConnectionInfo.getSSID();
    }

    public void stop() {
        client.setDecoratedClient(null);
        clearState();
        wifiLogger.unregister();
    }


    private static boolean isAlreadyConnectedToTargetNetwork(WifiInfo currentConnectionInfo,
                                                             String targetNetworkSsid) {
        return (isCurrentlyConnectedToAWifiNetwork(currentConnectionInfo)
                && targetNetworkSsid.equals(currentConnectionInfo.getSSID())
        );
    }

    private static boolean isCurrentlyConnectedToAWifiNetwork(WifiInfo currentConnectionInfo) {
        return (currentConnectionInfo != null
                && truthy(currentConnectionInfo.getSSID())
                && currentConnectionInfo.getNetworkId() != -1
                // yes, this happens.  Thanks, Android.
                && !"0x".equals(currentConnectionInfo.getSSID()));
    }

    private void scheduleTimeoutCheck(long timeoutInMillis, final WifiConfiguration config) {
        onTimeoutRunnable = () -> client.onApConnectionFailed(config);
        mainThreadHandler.postDelayed(onTimeoutRunnable, timeoutInMillis);
    }

    private void clearState() {
        if (onTimeoutRunnable != null) {
            mainThreadHandler.removeCallbacks(onTimeoutRunnable);
            onTimeoutRunnable = null;
        }

        if (wifiStateChangeListener != null) {
            appContext.unregisterReceiver(wifiStateChangeListener);
            wifiStateChangeListener = null;
        }

        for (Runnable runnable : setupRunnables) {
            mainThreadHandler.removeCallbacks(runnable);
        }
        setupRunnables.clear();
    }

    private void onWifiChangeBroadcastReceived(Intent intent, WifiConfiguration config) {
        // this will only be present if the new state is CONNECTED
        WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
        if (wifiInfo == null || wifiInfo.getSSID() == null) {
            // no WifiInfo or SSID means we're not interested.
            return;
        }
        log.i("Connected to: " + wifiInfo.getSSID());
        String ssid = wifiInfo.getSSID();
        if (ssid.equals(config.SSID) || WiFi.enQuotifySsid(ssid).equals(config.SSID)) {
            // FIXME: find a way to record success in memory in case this happens to happen
            // during a config change (etc)?
            client.onApConnectionSuccessful(config);
        }
    }


    // a Client decorator to ensure clearState() is called every time
    private class DecoratedClient implements Client {

        Client decoratedClient;

        @Override
        public void onApConnectionSuccessful(WifiConfiguration config) {
            clearState();
            if (decoratedClient != null) {
                decoratedClient.onApConnectionSuccessful(config);
            }
        }

        @Override
        public void onApConnectionFailed(WifiConfiguration config) {
            clearState();
            if (decoratedClient != null) {
                decoratedClient.onApConnectionFailed(config);
            }
        }

        void setDecoratedClient(Client decoratedClient) {
            this.decoratedClient = decoratedClient;
        }

    }

}
