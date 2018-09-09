package io.particle.android.sdk.devicesetup.setupsteps

import android.content.Context
import android.support.annotation.RestrictTo
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.devicesetup.commands.CommandClient
import io.particle.android.sdk.devicesetup.commands.ScanApCommand
import io.particle.android.sdk.devicesetup.ui.SuccessFragment
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.WifiFacade
import java.security.PublicKey

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SetupStepsFactory {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun newConfigureApStep(commandClient: CommandClient, reconnector: SetupStepApReconnector,
                           networkToConnectTo: ScanApCommand.Scan, networkSecretPlaintext: String,
                           publicKey: PublicKey): ConfigureAPStep {
        return ConfigureAPStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CONFIGURE_AP)
                        .setResultCode(SuccessFragment.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.configure_device_wifi_credentials)
                        .build(),
                commandClient, reconnector, networkToConnectTo, networkSecretPlaintext, publicKey)
    }


    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun newConnectDeviceToNetworkStep(commandClient: CommandClient, reconnector: SetupStepApReconnector): ConnectDeviceToNetworkStep {
        return ConnectDeviceToNetworkStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CONNECT_AP)
                        .setResultCode(SuccessFragment.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.connect_to_wifi_network)
                        .build(),
                commandClient, reconnector)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun newWaitForDisconnectionFromDeviceStep(deviceSoftApSsid: SSID,
                                              wifiFacade: WifiFacade): WaitForDisconnectionFromDeviceStep {
        return WaitForDisconnectionFromDeviceStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessFragment.RESULT_FAILURE_NO_DISCONNECT)
                        .setStepId(R.id.reconnect_to_wifi_network)
                        .build(),
                deviceSoftApSsid, wifiFacade)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun newEnsureSoftApNotVisible(deviceSoftApSsid: SSID?, wifiFacade: WifiFacade): EnsureSoftApNotVisible {
        return EnsureSoftApNotVisible(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessFragment.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.wait_for_device_cloud_connection)
                        .build(),
                deviceSoftApSsid, wifiFacade)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun newWaitForCloudConnectivityStep(context: Context): WaitForCloudConnectivityStep {
        return WaitForCloudConnectivityStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessFragment.RESULT_FAILURE_NO_DISCONNECT)
                        .setStepId(R.id.check_for_internet_connectivity)
                        .build(), context.applicationContext)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun newCheckIfDeviceClaimedStep(sparkCloud: ParticleCloud, deviceId: String): CheckIfDeviceClaimedStep {
        return CheckIfDeviceClaimedStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CLAIM)
                        .setResultCode(SuccessFragment.RESULT_FAILURE_CLAIMING)
                        .setStepId(R.id.verify_product_ownership)
                        .build(),
                sparkCloud, deviceId)
    }

    companion object {
        private const val MAX_RETRIES_CONFIGURE_AP = 5
        private const val MAX_RETRIES_CONNECT_AP = 5
        private const val MAX_RETRIES_DISCONNECT_FROM_DEVICE = 5
        private const val MAX_RETRIES_CLAIM = 5
    }
}
