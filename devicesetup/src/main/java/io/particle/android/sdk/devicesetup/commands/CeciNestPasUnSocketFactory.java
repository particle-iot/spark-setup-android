package io.particle.android.sdk.devicesetup.commands;

import java.io.IOException;
import java.net.Socket;

// Because it's not a java SocketFactory.
//
// This is going away soon, it exists temporarily just to get
// *something* in place to #SHIPIT.
@Deprecated
public interface CeciNestPasUnSocketFactory {

    Socket buildSocket(int readTimeoutMillis) throws IOException;

}
