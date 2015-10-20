package io.particle.android.sdk.devicesetup.setupsteps;

import android.content.Context;

import java.io.IOException;

import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.ConnectAPCommand;
import io.particle.android.sdk.devicesetup.commands.InterfaceBindingSocketFactory;


public class ConnectDeviceToNetworkStep extends SetupStep {

    private final CommandClient commandClient;
    private final Context ctx;

    private volatile boolean commandSent = false;

    public ConnectDeviceToNetworkStep(StepConfig stepConfig, CommandClient commandClient, Context ctx) {
        super(stepConfig);
        this.commandClient = commandClient;
        this.ctx = ctx;
    }

    @Override
    protected void onRunStep() throws SetupStepException {
        try {
            log.d("Sending connect-ap command");
            ConnectAPCommand.Response response = commandClient.sendCommandAndReturnResponse(
                    new ConnectAPCommand(0), ConnectAPCommand.Response.class,
                    new InterfaceBindingSocketFactory(ctx)
            );
            if (!response.isOK()) {
                throw new SetupStepException("ConnectAPCommand returned non-zero response code: " +
                        response.responseCode);
            }

            commandSent = true;

        } catch (IOException e) {
            throw new SetupStepException(e);
        }
    }

    @Override
    public boolean isStepFulfilled() {
        return commandSent;
    }

}
