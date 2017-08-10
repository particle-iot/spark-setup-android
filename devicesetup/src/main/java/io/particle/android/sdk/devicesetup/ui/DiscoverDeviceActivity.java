package io.particle.android.sdk.devicesetup.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.View;

import com.squareup.phrase.Phrase;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Locale;
import java.util.Set;

import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.devicesetup.ApConnector;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.DeviceIdCommand;
import io.particle.android.sdk.devicesetup.commands.PublicKeyCommand;
import io.particle.android.sdk.devicesetup.commands.SetCommand;
import io.particle.android.sdk.devicesetup.loaders.WifiScanResultLoader;
import io.particle.android.sdk.devicesetup.model.ScanResultNetwork;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepException;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.Crypto;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.ParticleDeviceSetupInternalStringUtils;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.android.sdk.utils.ui.WebViewActivity;

import static io.particle.android.sdk.utils.Py.truthy;


// FIXME: this activity is *far* too complicated.  Split it out into smaller components.
public class DiscoverDeviceActivity extends RequiresWifiScansActivity
        implements WifiListFragment.Client<ScanResultNetwork>, ApConnector.Client {


    // see ApConnector for the timeout value used for connecting to the soft AP
    private static final int MAX_NUM_DISCOVER_PROCESS_ATTEMPTS = 5;

    private static final TLog log = TLog.get(DiscoverDeviceActivity.class);


    private WifiFacade wifiFacade;
    private ParticleCloud sparkCloud;
    private DiscoverProcessWorker discoverProcessWorker;
    private SoftAPConfigRemover softAPConfigRemover;

    private WifiListFragment wifiListFragment;
    private ProgressDialog connectToApSpinnerDialog;

    private AsyncTask<Void, Void, SetupStepException> connectToApTask;
    private boolean isResumed = false;

    private int discoverProcessAttempts = 0;

    private SSID selectedSoftApSSID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_device);
        SEGAnalytics.screen("Device Setup: Device discovery screen");
        wifiFacade = WifiFacade.get(this);

        softAPConfigRemover = new SoftAPConfigRemover(this);
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();

        DeviceSetupState.previouslyConnectedWifiNetwork = wifiFacade.getCurrentlyConnectedSSID();

        sparkCloud = ParticleCloudSDK.getCloud();

        wifiListFragment = Ui.findFrag(this, R.id.wifi_list_fragment);
        ConnectToApFragment.ensureAttached(this);

        resetWorker();

        Ui.setText(this, R.id.wifi_list_header,
                Phrase.from(this, R.string.wifi_list_header_text)
                        .put("device_name", getString(R.string.device_name))
                        .format()
        );

        Ui.setText(this, R.id.msg_device_not_listed,
                Phrase.from(this, R.string.msg_device_not_listed)
                        .put("device_name", getString(R.string.device_name))
                        .put("setup_button_identifier", getString(R.string.mode_button_name))
                        .put("indicator_light", getString(R.string.indicator_light))
                        .put("indicator_light_setup_color_name", getString(R.string.listen_mode_led_color_name))
                        .format()
        );

        Ui.setTextFromHtml(this, R.id.action_troubleshooting, R.string.troubleshooting).setOnClickListener(
                v -> {
                    Uri uri = Uri.parse(v.getContext().getString(R.string.troubleshooting_uri));
                    startActivity(WebViewActivity.buildIntent(v.getContext(), uri));
                }
        );

        if (!truthy(sparkCloud.getLoggedInUsername())) {
            Ui.findView(this, R.id.logged_in_as).setVisibility(View.GONE);
        } else {
            Ui.setText(this, R.id.logged_in_as,
                    Phrase.from(this, R.string.you_are_logged_in_as)
                            .put("username", sparkCloud.getLoggedInUsername())
                            .format()
            );
        }

        Ui.findView(this, R.id.action_log_out).setVisibility(BaseActivity.setupOnly ? View.GONE : View.VISIBLE);
        Ui.findView(this, R.id.action_log_out).setOnClickListener(view -> {
            sparkCloud.logOut();
            log.i("logged out, username is: " + sparkCloud.getLoggedInUsername());
            startActivity(new Intent(DiscoverDeviceActivity.this, LoginActivity.class));
            finish();
        });

        Ui.findView(this, R.id.action_cancel).setOnClickListener(view -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!wifiFacade.isWifiEnabled()) {
            onWifiDisabled();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canGetLocation()) {
            onLocationDisabled();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = false;
    }

    private void resetWorker() {
        discoverProcessWorker = new DiscoverProcessWorker(
                CommandClient.newClientUsingDefaultsForDevices(this, selectedSoftApSSID)
        );
    }

    private void onLocationDisabled() {
        log.d("Location disabled; prompting user");
        new Builder(this).setTitle(R.string.location_required)
                .setMessage(R.string.location_required_message)
                .setPositiveButton(R.string.enable_location, ((dialog, which) -> {
                    dialog.dismiss();
                    log.i("Sending user to enabling Location services.");
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }))
                .setNegativeButton(R.string.exit_setup, ((dialog, which) -> {
                    dialog.dismiss();
                    finish();
                }))
                .show();
    }

    private boolean canGetLocation() {
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }
        return gpsEnabled || networkEnabled;
    }

    private void onWifiDisabled() {
        log.d("Wi-Fi disabled; prompting user");
        new AlertDialog.Builder(this)
                .setTitle(R.string.wifi_required)
                .setPositiveButton(R.string.enable_wifi, (dialog, which) -> {
                    dialog.dismiss();
                    log.i("Enabling Wi-Fi at the user's request.");
                    wifiFacade.setWifiEnabled(true);
                    wifiListFragment.scanAsync();
                })
                .setNegativeButton(R.string.exit_setup, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    @Override
    public void onNetworkSelected(ScanResultNetwork selectedNetwork) {
        WifiConfiguration wifiConfig = ApConnector.buildUnsecuredConfig(selectedNetwork.getSsid());
        selectedSoftApSSID = selectedNetwork.getSsid();
        resetWorker();
        connectToSoftAp(wifiConfig);
    }

    private void connectToSoftAp(WifiConfiguration config) {
        discoverProcessAttempts++;
        softAPConfigRemover.onSoftApConfigured(SSID.from(config.SSID));
        ConnectToApFragment.get(this).connectToAP(config);
        showProgressDialog();
    }

    @Override
    public Loader<Set<ScanResultNetwork>> createLoader(int id, Bundle args) {
        return new WifiScanResultLoader(this);
    }

    @Override
    public void onLoadFinished() {
        // no-op
    }

    @Override
    public String getListEmptyText() {
        return Phrase.from(this, R.string.empty_soft_ap_list_text)
                .put("device_name", getString(R.string.device_name))
                .format().toString();
    }

    @Override
    public int getAggroLoadingTimeMillis() {
        return 5000;
    }

    @Override
    public void onApConnectionSuccessful(WifiConfiguration config) {
        startConnectWorker();
    }

    @Override
    public void onApConnectionFailed(WifiConfiguration config) {
        hideProgressDialog();

        if (!canStartProcessAgain()) {
            onMaxAttemptsReached();
        } else {
            connectToSoftAp(config);
        }
    }

    private void showProgressDialog() {
        wifiListFragment.stopAggroLoading();

        String msg = Phrase.from(this, R.string.connecting_to_soft_ap)
                .put("device_name", getString(R.string.device_name))
                .format().toString();

        connectToApSpinnerDialog = new ProgressDialog(this);
        connectToApSpinnerDialog.setMessage(msg);
        connectToApSpinnerDialog.setCancelable(false);
        connectToApSpinnerDialog.setIndeterminate(true);
        connectToApSpinnerDialog.show();
    }

    private void hideProgressDialog() {
        wifiListFragment.startAggroLoading();
        if (connectToApSpinnerDialog != null) {
            if (!isFinishing()) {
                connectToApSpinnerDialog.dismiss();
            }
            connectToApSpinnerDialog = null;
        }
    }

    private void startConnectWorker() {
        // first, make sure we haven't actually been called twice...
        if (connectToApTask != null) {
            log.d("Already running connect worker " + connectToApTask + ", refusing to start another");
            return;
        }

        wifiListFragment.stopAggroLoading();
        // FIXME: verify first that we're still connected to the intended network
        if (!canStartProcessAgain()) {
            hideProgressDialog();
            onMaxAttemptsReached();
            return;
        }

        discoverProcessAttempts++;

        // This just has doInBackground() return null on success, or if an
        // exception was thrown, it passes that along instead to indicate failure.
        connectToApTask = new AsyncTask<Void, Void, SetupStepException>() {

            @Override
            protected SetupStepException doInBackground(Void... voids) {
                try {
                    // including this sleep because without it,
                    // we seem to attempt a socket connection too early,
                    // and it makes the process time out(!)
                    log.d("Waiting a couple seconds before trying the socket connection...");
                    EZ.threadSleep(2000);
                    discoverProcessWorker.doTheThing();
                    return null;

                } catch (SetupStepException e) {
                    log.d("Setup exception thrown: ", e);
                    return e;

                }
            }

            @Override
            protected void onPostExecute(SetupStepException error) {
                connectToApTask = null;
                if (error == null || (BaseActivity.setupOnly && error instanceof DeviceAlreadyClaimed)) {
                    // no exceptions thrown, huzzah
                    hideProgressDialog();
                    startActivity(SelectNetworkActivity.buildIntent(
                            DiscoverDeviceActivity.this, selectedSoftApSSID));
                    finish();
                } else if (error instanceof DeviceAlreadyClaimed) {
                    hideProgressDialog();
                    onDeviceClaimedByOtherUser();
                } else {
                    // nope, do it all over again.
                    // FIXME: this might be a good time to display some feedback...
                    startConnectWorker();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean canStartProcessAgain() {
        return discoverProcessAttempts < MAX_NUM_DISCOVER_PROCESS_ATTEMPTS;
    }

    private void onMaxAttemptsReached() {
        if (!isResumed) {
            finish();
            return;
        }

        String errorMsg = Phrase.from(this, R.string.unable_to_connect_to_soft_ap)
                .put("device_name", getString(R.string.device_name))
                .format().toString();

        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(errorMsg)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    startActivity(new Intent(DiscoverDeviceActivity.this, GetReadyActivity.class));
                    finish();
                })
                .show();
    }

    private void onDeviceClaimedByOtherUser() {
        String dialogMsg = getString(R.string.dialog_title_owned_by_another_user,
                getString(R.string.device_name), sparkCloud.getLoggedInUsername());

        new Builder(this)
                .setTitle(getString(R.string.change_owner_question))
                .setMessage(dialogMsg)
                .setPositiveButton(getString(R.string.change_owner),
                        (dialog, which) -> {
                            dialog.dismiss();
                            log.i("Changing owner to " + sparkCloud.getLoggedInUsername());
//                        // FIXME: state mutation from another class.  Not pretty.
//                        // Fix this by breaking DiscoverProcessWorker down into Steps
                            resetWorker();
                            discoverProcessWorker.needToClaimDevice = true;
                            discoverProcessWorker.gotOwnershipInfo = true;
                            discoverProcessWorker.isDetectedDeviceClaimed = false;
                            DeviceSetupState.deviceNeedsToBeClaimed = true;

                            showProgressDialog();
                            startConnectWorker();
                        })
                .setNegativeButton(R.string.cancel,
                        (dialog, which) -> {
                            dialog.dismiss();
                            startActivity(new Intent(DiscoverDeviceActivity.this, GetReadyActivity.class));
                            finish();
                        })
                .show();
    }


    // FIXME: Even before it's done, I am pretty sure this will need
    // to go through a round of "solve et coagula" before it's
    // really right, at least maintenance-wise.
    // FIXME: this naming is no longer really applicable.
    static class DiscoverProcessWorker {

        private final CommandClient client;

        private volatile String detectedDeviceID;

        private volatile boolean isDetectedDeviceClaimed;
        private volatile boolean gotOwnershipInfo;
        private volatile boolean needToClaimDevice;

        DiscoverProcessWorker(CommandClient client) {
            this.client = client;
        }

        // FIXME: all this should probably become a list of commands to run in a queue,
        // each with shortcut conditions for when they've already been fulfilled, instead of
        // this if-else/try-catch ladder.
        public void doTheThing() throws SetupStepException {
            // 1. get device ID
            if (!truthy(detectedDeviceID)) {
                try {
                    DeviceIdCommand.Response response = client.sendCommand(
                            new DeviceIdCommand(), DeviceIdCommand.Response.class);
                    detectedDeviceID = response.deviceIdHex.toLowerCase(Locale.ROOT);
                    DeviceSetupState.deviceToBeSetUpId = detectedDeviceID;
                    isDetectedDeviceClaimed = truthy(response.isClaimed);
                } catch (IOException e) {
                    throw new SetupStepException("Process died while trying to get the device ID", e);
                }
            }

            // 2. Get public key
            if (DeviceSetupState.publicKey == null) {
                try {
                    DeviceSetupState.publicKey = getPublicKey();
                } catch (Crypto.CryptoException e) {
                    throw new SetupStepException("Unable to get public key: ", e);

                } catch (IOException e) {
                    throw new SetupStepException("Error while fetching public key: ", e);
                }
            }

            // 3. check ownership
            //
            // all cases:
            // (1) device not claimed `c=0` — device should also not be in list from API => mobile
            //      app assumes user is claiming
            // (2) device claimed `c=1` and already in list from API => mobile app does not ask
            //      user about taking ownership because device already belongs to this user
            // (3) device claimed `c=1` and NOT in the list from the API => mobile app asks whether
            //      use would like to take ownership
            if (!gotOwnershipInfo) {
                needToClaimDevice = false;

                // device was never claimed before - so we need to claim it anyways
                if (!isDetectedDeviceClaimed) {
                    setClaimCode();
                    needToClaimDevice = true;

                } else {
                    boolean deviceClaimedByUser = false;
                    for (String deviceId : DeviceSetupState.claimedDeviceIds) {
                        if (deviceId.equalsIgnoreCase(detectedDeviceID)) {
                            deviceClaimedByUser = true;
                            break;
                        }
                    }
                    gotOwnershipInfo = true;

                    if (isDetectedDeviceClaimed && !deviceClaimedByUser) {
                        // This device is already claimed by someone else. Ask the user if we should
                        // change ownership to the current logged in user, and if so, set the claim code.

                        throw new DeviceAlreadyClaimed("Device already claimed by another user");

                    } else {
                        // Success: no exception thrown, this part of the process is complete.
                        // Let the caller continue on with the setup process.
                        return;
                    }
                }

            } else {
                if (needToClaimDevice) {
                    setClaimCode();
                }
                // Success: no exception thrown, the part of the process is complete.  Let the caller
                // continue on with the setup process.
                return;
            }
        }

        private void setClaimCode() throws SetupStepException {
            try {
                log.d("Setting claim code using code: " + DeviceSetupState.claimCode);

                String claimCodeNoBackslashes = ParticleDeviceSetupInternalStringUtils.remove(
                        DeviceSetupState.claimCode, "\\");
                SetCommand.Response response = client.sendCommand(
                        new SetCommand("cc", claimCodeNoBackslashes), SetCommand.Response.class);

                if (truthy(response.responseCode)) {
                    // a non-zero response indicates an error, ala UNIX return codes
                    throw new SetupStepException("Received non-zero return code from set command: "
                            + response.responseCode);
                }

                log.d("Successfully set claim code");

            } catch (IOException e) {
                throw new SetupStepException(e);
            }
        }

        private PublicKey getPublicKey() throws Crypto.CryptoException, IOException {
            PublicKeyCommand.Response response = this.client.sendCommand(
                    new PublicKeyCommand(), PublicKeyCommand.Response.class);
            return Crypto.readPublicKeyFromHexEncodedDerString(response.publicKey);
        }
    }


    // FIXME: remove this if we break down the worker above into Steps
    // no data to pass along with this at the moment, I just want to specify
    // that this isn't an error which should necessarily count against retries.
    static class DeviceAlreadyClaimed extends SetupStepException {

        DeviceAlreadyClaimed(String msg) {
            super(msg);
        }

    }

}
