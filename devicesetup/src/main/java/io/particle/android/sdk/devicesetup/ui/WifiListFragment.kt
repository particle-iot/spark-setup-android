package io.particle.android.sdk.devicesetup.ui


import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ListFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.model.WifiNetwork
import io.particle.android.sdk.utils.EZ
import io.particle.android.sdk.utils.Py.set
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.Ui
import java.util.ArrayList
import kotlin.Comparator


class WifiListFragment<T : WifiNetwork> : ListFragment(), LoaderManager.LoaderCallbacks<Set<T>> {
    private var adapter: WifiNetworkAdapter? = null
    private var aggroLoadingRunnable: Runnable? = null
    private var aggroLoadingHandler: Handler? = null
    private var client: Client<T>? = null

    private var previousData: Set<T> = set()


    interface Client<T : WifiNetwork> {

        val listEmptyText: String

        val aggroLoadingTimeMillis: Int

        fun onNetworkSelected(selectedNetwork: T?)

        fun createLoader(id: Int, args: Bundle?): Loader<Set<T>>

        fun onLoadFinished()

    }

    fun scanAsync() {
        if (isDetached || client == null) {
            stopAggroLoading()
        } else {
            // FIXME: just use a rsrc ID for the loader ID instead of this madness.
            val loader = loaderManager.getLoader<Any>(javaClass.hashCode())
            loader!!.forceLoad()
        }
    }

    private inline fun <reified R : Client<T>> getClientClass() = R::class.java

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        client = EZ.getCallbacksOrThrow<Client<T>>(this, getClientClass())
        if (aggroLoadingHandler == null) {
            aggroLoadingHandler = Handler()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = WifiNetworkAdapter(activity!!)
        setEmptyText(client!!.listEmptyText)
        loaderManager.initLoader(javaClass.hashCode(), null, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listView.isVerticalScrollBarEnabled = true
        listView.isScrollbarFadingEnabled = false
    }

    override fun onStart() {
        super.onStart()
        scanAsync()
        startAggroLoading()
    }

    override fun onStop() {
        super.onStop()
        stopAggroLoading()
    }

    override fun onDetach() {
        client = null
        super.onDetach()
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)

        val selectedNetwork = listAdapter!!.getItem(position)
        if (null != client) {
            client!!.onNetworkSelected(selectedNetwork as T?)
        } else {
            log.e("Client was null")
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Set<T>> {
        return client!!.createLoader(id, args)
    }

    override fun onLoadFinished(loader: Loader<Set<T>>, data: Set<T>?) {
        val data = data
        log.d("new scan results: " + data!!)

        // only do this work if our data has actually changed
        if (previousData != data) {
            previousData = data
            adapter!!.clear()

            val asList = ArrayList(data)
            asList.sortWith(Comparator { lhs, rhs -> lhs.ssid!!.compareTo(rhs.ssid!!) })

            adapter!!.addAll(asList)
        }

        // setting the adapter at this point, instead of in onCreateView(), etc, means we get
        // a loading spinner for free -- see ListFragment source
        if (listAdapter == null) {  // no list shown yet
            listAdapter = adapter
        }

        if (client != null) {
            client!!.onLoadFinished()
        }
    }

    override fun onLoaderReset(loader: Loader<Set<T>>) {
        adapter!!.clear()
    }


    fun startAggroLoading() {
        if (aggroLoadingRunnable == null) {
            scheduleNextAggroLoad()
        }
    }

    private fun scheduleNextAggroLoad() {
        if (client == null) {
            stopAggroLoading()
            return
        }

        aggroLoadingRunnable = Runnable {
            log.d("Running aggro loading")
            scanAsync()
            aggroLoadingRunnable = null
            scheduleNextAggroLoad()
        }
        aggroLoadingHandler!!.postDelayed(aggroLoadingRunnable, client!!.aggroLoadingTimeMillis.toLong())
    }

    fun stopAggroLoading() {
        if (aggroLoadingRunnable != null) {
            aggroLoadingHandler!!.removeCallbacks(aggroLoadingRunnable)
            aggroLoadingRunnable = null
        }
    }


    private inner class WifiNetworkAdapter internal constructor(context: Context) : ArrayAdapter<T>(context, android.R.layout.simple_list_item_1) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.row_wifi_scan_result, parent, false)

                val theWifi = Ui.findView<ImageView>(convertView!!, R.id.the_wifi)
                val whiteWifi = Ui.getTintedDrawable(context, R.drawable.the_wifi,
                        R.color.element_tint_color)
                theWifi.setImageDrawable(whiteWifi)

                val securityIcon = Ui.findView<ImageView>(convertView, R.id.wifi_security_indicator_icon)
                val whiteLock = Ui.getTintedDrawable(context, R.drawable.lock,
                        R.color.element_tint_color)
                securityIcon.setImageDrawable(whiteLock)
            }

            val wifiNetwork = getItem(position)
            if (wifiNetwork != null) {
                Ui.setText(convertView, android.R.id.text1, wifiNetwork.ssid.toString())
                Ui.findView<View>(convertView, R.id.wifi_security_indicator_icon).visibility = if (wifiNetwork.isSecured) View.VISIBLE else View.GONE
            }
            return convertView
        }

    }

    companion object {
        private val log = TLog.get(WifiListFragment::class.java)
    }

}
