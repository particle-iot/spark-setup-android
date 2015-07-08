package io.particle.android.sdk.devicesetup.model;

import android.net.wifi.ScanResult;

import java.util.Set;

import static io.particle.android.sdk.utils.Py.set;


// FIXME: this naming... is not ideal.
public class ScanResultNetwork implements WifiNetwork {

    private static final Set<String> wifiSecurityTypes = set("WEP", "PSK", "EAP");


    public final ScanResult scanResult;

    public ScanResultNetwork(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    @Override
    public String getSsid() {
        return scanResult.SSID;
    }

    @Override
    public boolean isSecured() {
        // <sad trombone>
        // this seems like a bad joke of an "API", but this is basically what
        // Android does internally (see: http://goo.gl/GCRIKi)
        for (String securityType : wifiSecurityTypes) {
            if (scanResult.capabilities.contains(securityType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScanResultNetwork that = (ScanResultNetwork) o;

        if (getSsid() != null ? !getSsid().equals(that.getSsid()) : that.getSsid() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getSsid() != null ? getSsid().hashCode() : 0;
    }
}
