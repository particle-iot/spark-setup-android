package io.particle.android.sdk.di;

import android.support.annotation.RestrictTo;

import dagger.Component;
import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.accountsetup.PasswordResetActivity;
import io.particle.android.sdk.devicesetup.ui.ConnectToApFragment;
import io.particle.android.sdk.devicesetup.ui.ConnectingActivity;
import io.particle.android.sdk.devicesetup.ui.DiscoverDeviceActivity;
import io.particle.android.sdk.devicesetup.ui.GetReadyActivity;
import io.particle.android.sdk.devicesetup.ui.ManualNetworkEntryActivity;
import io.particle.android.sdk.devicesetup.ui.PasswordEntryActivity;
import io.particle.android.sdk.devicesetup.ui.SelectNetworkActivity;
import io.particle.android.sdk.devicesetup.ui.SuccessActivity;

@PerActivity
@Component(modules = {ApModule.class}, dependencies = ApplicationComponent.class)
@RestrictTo({RestrictTo.Scope.LIBRARY})
public interface ActivityInjectorComponent {
    void inject(GetReadyActivity activity);

    void inject(LoginActivity loginActivity);

    void inject(PasswordResetActivity passwordResetActivity);

    void inject(SuccessActivity successActivity);

    void inject(DiscoverDeviceActivity discoverDeviceActivity);

    void inject(ConnectingActivity connectingActivity);

    void inject(PasswordEntryActivity passwordEntryActivity);

    void inject(ManualNetworkEntryActivity manualNetworkEntryActivity);

    void inject(SelectNetworkActivity selectNetworkActivity);

    void inject(ConnectToApFragment connectToApFragment);

}
