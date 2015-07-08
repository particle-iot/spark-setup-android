package io.particle.android.sdk.utils;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static io.particle.android.sdk.utils.Py.truthy;


public class WiFi {

    private static final TLog log = TLog.get(WiFi.class);


    public static Predicate<ScanResult> is24Ghz = new Predicate<ScanResult>() {

        @Override
        public boolean apply(ScanResult input) {
            // values taken from the ScanResult source
            return input.frequency > 2300 && input.frequency < 2500;
        }
    };


    // "truthy": see Py.truthy() javadoc
    // tl;dr:  not null or "" (empty string)
    public static Predicate<ScanResult> isWifiNameTruthy = new Predicate<ScanResult>() {

        @Override
        public boolean apply(ScanResult input) {
            return truthy(input.SSID);
        }
    };


    public static void reenableNetwork(String ssid, Context ctx) {
        WifiManager wifiManager = getWifiManager(ctx);
        int networkId = getConfiguredNetworkId(ssid, ctx);
        if (networkId != -1) {
            log.d("Reenabling network configuration for:" + ssid);
            wifiManager.enableNetwork(networkId, false);
        } else {
            log.d("No network found for SSID " + ssid);
        }
    }

    public static void removeNetwork(String ssid, Context ctx) {
        WifiManager wifiManager = getWifiManager(ctx);
        int networkId = getConfiguredNetworkId(ssid, ctx);
        if (networkId != -1) {
            log.d("Removing network configuration for:" + ssid);
            wifiManager.removeNetwork(networkId);
        } else {
            log.d("No network found for SSID " + ssid);
        }
    }

    public static String getCurrentlyConnectedSSID(Context ctx) {
        WifiManager wifiManager = getWifiManager(ctx);
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo == null) {
            log.d("getCurrentlyConnectedSSID(): " +
                    "WifiManager.getConnectionInfo() returned null");
            return null;
        } else {
            String ssid = deQuotifySsid(connectionInfo.getSSID());
            log.d("Currently connected to: " + ssid +
                    ", supplicant state: " + connectionInfo.getSupplicantState());
            return ssid;
        }
    }

    public static String enQuotifySsid(String SSID) {
        Preconditions.checkNotNull(SSID, "'SSID' cannot be null!");
        final String quoteMark = "\"";
        if (!SSID.startsWith(quoteMark) && !SSID.endsWith(quoteMark)) {
            SSID = quoteMark + SSID + quoteMark;
        }
        return SSID;
    }

    // Because Android thought it was a good idea to return SSIDs with quotes around them...
    public static String deQuotifySsid(String SSID) {
        // FIXME: remove null check later
        if (SSID == null) {
            return null;
        }
        String quoteMark = "\"";
        SSID = StringUtils.removeStart(SSID, quoteMark);
        SSID = StringUtils.removeEnd(SSID, quoteMark);
        return SSID;
    }

    public static int getConfiguredNetworkId(String SSID, Context ctx) {
        WifiManager wifiManager = getWifiManager(ctx);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration config : configuredNetworks) {
                if (SSID.equalsIgnoreCase(config.SSID)) {
                    return config.networkId;
                }
            }
        }
        return -1;
    }

    private static WifiManager getWifiManager(Context ctx) {
        return (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    }

}
