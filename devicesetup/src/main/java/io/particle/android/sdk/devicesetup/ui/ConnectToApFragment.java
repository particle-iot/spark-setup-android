package io.particle.android.sdk.devicesetup.ui;

import android.app.Activity;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import io.particle.android.sdk.devicesetup.ApConnector;
import io.particle.android.sdk.devicesetup.ApConnector.Client;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WorkerFragment;
import io.particle.android.sdk.utils.ui.Ui;


// reconsider if this even needs to be a fragment at all
public class ConnectToApFragment extends WorkerFragment {

    public static final String TAG = WorkerFragment.buildFragmentTag(ConnectToApFragment.class);


    public static ConnectToApFragment get(FragmentActivity activity) {
        return Ui.findFrag(activity, TAG);
    }

    public static ConnectToApFragment ensureAttached(FragmentActivity activity) {
        ConnectToApFragment frag = get(activity);
        if (frag == null) {
            frag = new ConnectToApFragment();
            WorkerFragment.addFragment(activity, frag, TAG);
        }
        return frag;
    }

    private ApConnector apConnector;
    private Client apConnectorClient;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        apConnectorClient = EZ.getCallbacksOrThrow(this, Client.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apConnector = new ApConnector(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        apConnector.stop();
    }

    /**
     * Connect this Android device to the specified AP.
     *
     * @param config the WifiConfiguration defining which AP to connect to
     * @return the SSID that was connected prior to calling this method.  Will be null if
     * there was no network connected, or if already connected to the target network.
     */
    public SSID connectToAP(final WifiConfiguration config) {
        return apConnector.connectToAP(config, apConnectorClient);
    }

}
