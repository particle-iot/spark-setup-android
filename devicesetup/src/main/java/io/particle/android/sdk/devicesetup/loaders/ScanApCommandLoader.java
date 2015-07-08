package io.particle.android.sdk.devicesetup.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import java.io.IOException;
import java.util.Set;

import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.utils.TLog;

import static io.particle.android.sdk.utils.Py.set;

/**
 * Returns the results of the "scan-ap" command from the device.
 * <p/>
 * Will return null if an exception is thrown when trying to send the command
 * and receive a reply from the device.
 */
public class ScanApCommandLoader extends AsyncTaskLoader<Set<ScanAPCommandResult>> {

    private static final TLog log = TLog.get(ScanApCommandLoader.class);

    private final CommandClient commandClient;
    private final Set<ScanAPCommandResult> accumulatedResults = set();

    public ScanApCommandLoader(Context context, CommandClient client) {
        super(context);
        commandClient = client;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public Set<ScanAPCommandResult> loadInBackground() {
        try {
            ScanApCommand.Response response = commandClient.sendCommandAndReturnResponse(
                    new ScanApCommand(), ScanApCommand.Response.class);
            accumulatedResults.addAll(
                    FluentIterable.from(response.getScans())
                            .transform(toWifiNetwork)
                            .toSet());
            log.d("Latest accumulated scan results: " + accumulatedResults);
            return set(accumulatedResults);

        } catch (IOException e) {
            log.e("Error running scan-ap command: ", e);
            return null;
        }
    }

    private static Function<ScanApCommand.Scan, ScanAPCommandResult> toWifiNetwork =
            new Function<ScanApCommand.Scan, ScanAPCommandResult>() {

        @Override
        public ScanAPCommandResult apply(ScanApCommand.Scan input) {
            return new ScanAPCommandResult(input);
        }
    };

}
