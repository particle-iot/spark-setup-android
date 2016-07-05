package io.particle.devicesetup.exampleapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.model.DeviceCustomization;
import io.particle.android.sdk.utils.ui.Ui;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ParticleDeviceSetupLibrary.init(this.getApplicationContext(), MainActivity.class);

        Ui.findView(this, R.id.start_setup_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invokeDeviceSetupWithCustomization();
            }
        });
    }

    public void invokeDeviceSetup() {
        ParticleDeviceSetupLibrary.startDeviceSetup(this);
    }

    public void invokeDeviceSetupWithCustomization() {
        DeviceCustomization dc = new DeviceCustomization();
        dc.setBrandName(R.string.company_name);
        dc.setDeviceName(R.string.device);
        dc.setNetworkNamePrefix(R.string.prefix);
        dc.setDeviceImageSmall(R.drawable.device_pic);
        ParticleDeviceSetupLibrary.startDeviceSetup(this, dc);
    }
}
