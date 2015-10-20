package io.particle.android.sdk.devicesetup.commands;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.TLog;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import static io.particle.android.sdk.utils.Py.truthy;


public class CommandClient {

    public static final int DEFAULT_TIMEOUT_SECONDS = 10;


    public static CommandClient newClient(@NonNull InetSocketAddress socketAddress) {
        return new CommandClient(socketAddress);
    }

    // FIXME: set these defaults in a resource file
    public static CommandClient newClientUsingDefaultSocketAddress() {
        return newClient(new InetSocketAddress("192.168.0.1", 5609));
    }


    private static final TLog log = TLog.get(CommandClient.class);
    private static final Gson gson = new Gson();


    private final InetSocketAddress deviceAddress;


    // no public constructor; use the factory methods above
    private CommandClient(InetSocketAddress deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public void sendCommand(Command command, CeciNestPasUnSocketFactory socketFactory) throws IOException {
        sendAndMaybeReceive(command, Void.class, socketFactory);
    }

    public <T> T sendCommandAndReturnResponse(Command command, Class<T> responseType,
                                              CeciNestPasUnSocketFactory socketFactory)
            throws IOException {
        return sendAndMaybeReceive(command, responseType, socketFactory);
    }


    private <T> T sendAndMaybeReceive(Command command, Class<T> responseType,
                                      CeciNestPasUnSocketFactory socketFactory) throws IOException {
        log.i("Preparing to send command '" + command.getCommandName() + "'");
        String commandData = buildCommandData(command);

        BufferedSink buffer = null;
        try {
            // send command
            int timeoutMillis = DEFAULT_TIMEOUT_SECONDS * 1000;
            Socket socket = socketFactory.buildSocket(timeoutMillis);
            socket.connect(deviceAddress, timeoutMillis);
            buffer = wrapSocket(socket, DEFAULT_TIMEOUT_SECONDS);
            log.d("Writing command data");
            buffer.writeUtf8(commandData);
            buffer.flush();

            // if no response defined, just exit early.
            if (responseType.equals(Void.class)) {
                log.d("Done.");
                return null;
            }

            return readResponse(socket, responseType, DEFAULT_TIMEOUT_SECONDS);

        } finally {
            EZ.closeThisThingOrMaybeDont(buffer);
        }
    }

    private BufferedSink wrapSocket(Socket socket, int timeoutValueInSeconds) throws IOException {
        BufferedSink sink = Okio.buffer(Okio.sink(socket));
        sink.timeout().timeout(timeoutValueInSeconds, TimeUnit.SECONDS);
        return sink;
    }

    private String buildCommandData(Command command) {
        StringBuilder commandData = new StringBuilder()
                .append(command.getCommandName())
                .append("\n");

        String commandArgs = command.argsAsJsonString(gson);
        if (truthy(commandArgs)) {
            commandData.append(commandArgs.length());
            commandData.append("\n\n");
            commandData.append(commandArgs);
        } else {
            commandData.append("0\n\n");
        }

        String built = commandData.toString();
        log.i("*** BUILT COMMAND DATA: '" + StringEscapeUtils.escapeJava(built) + "'");
        return built;
    }

    private <T> T readResponse(Socket socket, Class<T> responseType, int timeoutValueInSeconds)
            throws IOException {
        BufferedSource buffer = Okio.buffer(Okio.source(socket));
        buffer.timeout().timeout(timeoutValueInSeconds, TimeUnit.SECONDS);

        log.d("Reading response data...");
        String line;
        do {
            // read (and throw away, for now) any headers
            line = buffer.readUtf8LineStrict();
        } while (truthy(line));

        String responseData = buffer.readUtf8();
        log.d("Command response (raw): " + StringEscapeUtils.escapeJava(responseData));
        T tee = gson.fromJson(responseData, responseType);
        log.d("Command response: " + tee);
        EZ.closeThisThingOrMaybeDont(buffer);
        return tee;
    }

}
