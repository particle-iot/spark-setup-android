package io.particle.android.sdk.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import static io.particle.android.sdk.utils.Py.set;


public class SoftAPConfigRemover {


    private static final TLog log = TLog.get(SoftAPConfigRemover.class);


    private static final String PREFS_SOFT_AP_NETWORK_REMOVER = "PREFS_SOFT_AP_NETWORK_REMOVER";

    private static final String KEY_SOFT_AP_SSIDS = "KEY_SOFT_AP_SSIDS";
    private static final String KEY_DISABLED_WIFI_SSIDS = "KEY_DISABLED_WIFI_SSIDS";


    private final Context ctx;
    private final SharedPreferences prefs;

    public SoftAPConfigRemover(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        prefs = ctx.getSharedPreferences(PREFS_SOFT_AP_NETWORK_REMOVER, Context.MODE_PRIVATE);
    }

    public void onSoftApConfigured(String newSsid) {
        // make a defensive copy of what we get back
        Set<String> ssids = set(loadSSIDStringSetWithKey(KEY_SOFT_AP_SSIDS));
        ssids.add(newSsid);
        saveSSIDsWithKey(KEY_SOFT_AP_SSIDS, ssids);
    }

    public void removeAllSoftApConfigs() {
        for (String ssid : loadSSIDStringSetWithKey(KEY_SOFT_AP_SSIDS)) {
            WiFi.removeNetwork(ssid, ctx);
        }
        saveSSIDsWithKey(KEY_SOFT_AP_SSIDS, new HashSet<String>());
    }

    public void onWifiNetworkDisabled(String ssid) {
        log.v("onWifiNetworkDisabled() " + ssid);
        Set<String> ssids = set(loadSSIDStringSetWithKey(KEY_DISABLED_WIFI_SSIDS));
        ssids.add(ssid);
        saveSSIDsWithKey(KEY_DISABLED_WIFI_SSIDS, ssids);
    }

    public void reenableWifiNetworks() {
        log.v("reenableWifiNetworks()");
        for (String ssid : loadSSIDStringSetWithKey(KEY_DISABLED_WIFI_SSIDS)) {
            WiFi.reenableNetwork(ssid, ctx);
        }
        saveSSIDsWithKey(KEY_DISABLED_WIFI_SSIDS, new HashSet<String>());
    }


    private Set<String> loadSSIDStringSetWithKey(String key) {
        log.v("loadSSIDStringSetWithKey(" + key + ")");

        Set<String> ssids = set();
        ssids = prefs.getStringSet(key, ssids);
        log.v("Loaded saved SSIDS: " + ssids);

        Set<String> diffQuotes = set();
        for (String ssid : ssids) {
            diffQuotes.add(WiFi.enQuotifySsid(ssid));
            diffQuotes.add(WiFi.deQuotifySsid(ssid));
        }
        ssids.addAll(diffQuotes);

        log.v("Returning SSIDS: " + ssids);
        return ssids;
    }

    @SuppressLint("CommitPrefEdits")
    private void saveSSIDsWithKey(String key, Set<String> ssids) {
        log.v("saveSSIDsWithKey() " + key + ", " + ssids);
        prefs.edit()
                .putStringSet(key, ssids)
                .commit();
    }
}
