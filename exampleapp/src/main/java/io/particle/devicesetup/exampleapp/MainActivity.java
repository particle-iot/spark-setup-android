package io.particle.devicesetup.exampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.utils.ui.Ui;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_SETUP_LAUNCHED_TIME = "io.particle.devicesetup.exampleapp.SETUP_LAUNCHED_TIME";
    public final static int SETUP_REQUEST = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ParticleDeviceSetupLibrary.init(this.getApplicationContext());

        Ui.findView(this, R.id.start_setup_button).setOnClickListener(view -> invokeDeviceSetup());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (SETUP_REQUEST == requestCode && resultCode == RESULT_OK) {
            String setupLaunchTime = data.getStringExtra(EXTRA_SETUP_LAUNCHED_TIME);

            if (setupLaunchTime != null) {
                TextView label = Ui.findView(this, R.id.textView);

                label.setText(String.format(getString(R.string.welcome_back), setupLaunchTime));
            }
        }
    }

    public void invokeDeviceSetup() {
        ParticleDeviceSetupLibrary.startDeviceSetup(this, SETUP_REQUEST);
    }

}
