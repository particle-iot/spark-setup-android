package io.particle.android.sdk.devicesetup.ui

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.os.Bundle
import android.support.v4.app.Fragment
import io.particle.android.sdk.devicesetup.ApConnector
import io.particle.android.sdk.devicesetup.ApConnector.Client
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.di.ApModule
import io.particle.android.sdk.utils.EZ
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.WifiFacade
import io.particle.android.sdk.utils.WorkerFragment
import io.particle.android.sdk.utils.ui.Ui
import javax.inject.Inject


// reconsider if this even needs to be a fragment at all
class ConnectToApFragment : WorkerFragment() {

    @Inject
    lateinit var apConnector: ApConnector
    @Inject
    lateinit var wifiFacade: WifiFacade
    private lateinit var apConnectorClient: Client

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        apConnectorClient = EZ.getCallbacksOrThrow(this, Client::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ParticleDeviceSetupLibrary.getInstance().applicationComponent
                .activityComponentBuilder()
                .apModule(ApModule())
                .build()
                .inject(this)
    }

    override fun onStop() {
        super.onStop()
        apConnector.stop()
    }

    /**
     * Connect this Android device to the specified AP.
     *
     * @param config the WifiConfiguration defining which AP to connect to
     * @return the SSID that was connected prior to calling this method.  Will be null if
     * there was no network connected, or if already connected to the target network.
     */
    fun connectToAP(config: WifiConfiguration): SSID? {
        return apConnector.connectToAP(config, apConnectorClient)
    }

    companion object {
        private val TAG = WorkerFragment.buildFragmentTag(ConnectToApFragment::class.java)

        operator fun get(fragment: Fragment): ConnectToApFragment? {
            return Ui.findFrag(fragment, TAG)
        }

        fun ensureAttached(fragment: Fragment): ConnectToApFragment {
            var frag: ConnectToApFragment? = get(fragment)
            if (frag == null) {
                frag = ConnectToApFragment()
                WorkerFragment.addFragment(fragment, frag, TAG)
            }
            return frag
        }
    }

}
