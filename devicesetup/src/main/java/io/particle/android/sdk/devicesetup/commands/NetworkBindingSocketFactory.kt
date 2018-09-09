package io.particle.android.sdk.devicesetup.commands

import android.annotation.TargetApi
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.WifiFacade
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import javax.net.SocketFactory

/**
 * Factory for Sockets which binds communication to a particular [android.net.Network]
 */
class NetworkBindingSocketFactory(private val wifiFacade: WifiFacade,
                                  private val softAPSSID: SSID?, // used as connection timeout and read timeout
                                  private val timeoutMillis: Int) : SocketFactory() {

    @Throws(IOException::class)
    override fun createSocket(): Socket {
        return buildSocket()
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket {
        val socket = buildSocket()
        socket.connect(InetSocketAddress(host, port), timeoutMillis)
        return socket
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        throw UnsupportedOperationException(
                "Specifying a localHost or localPort arg is not supported.")
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        val socket = buildSocket()
        socket.connect(InetSocketAddress(host, port), timeoutMillis)
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress,
                              localPort: Int): Socket {
        throw UnsupportedOperationException(
                "Specifying a localHost or localPort arg is not supported.")
    }


    @Throws(IOException::class)
    private fun buildSocket(): Socket {
        val socket = Socket()
        socket.soTimeout = timeoutMillis

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            bindSocketToSoftAp(socket)
        }

        return socket
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    @Throws(IOException::class)
    private fun bindSocketToSoftAp(socket: Socket) {
        try {
            val softAp = wifiFacade.getNetworkForSSID(softAPSSID)
                    ?: // If this ever fails, fail VERY LOUDLY to make sure we hear about it...
                    // FIXME: report this error via analytics
                    throw SocketBindingException("Could not find Network for SSID $softAPSSID")

            softAp.bindSocket(socket)
        } catch (ex: NullPointerException) {
            throw SocketBindingException("Could not find Network for SSID $softAPSSID")
        }

    }


    private class SocketBindingException internal constructor(msg: String) : IOException(msg)

    companion object {
        private val log = TLog.get(NetworkBindingSocketFactory::class.java)
    }

}
