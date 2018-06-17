package io.particle.android.sdk.di;

import android.support.annotation.RestrictTo;

import dagger.Subcomponent;
import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.accountsetup.LoginFragment;
import io.particle.android.sdk.accountsetup.PasswordResetActivity;
import io.particle.android.sdk.accountsetup.PasswordResetFragment;
import io.particle.android.sdk.devicesetup.ui.ConnectToApFragment;
import io.particle.android.sdk.devicesetup.ui.ConnectingActivity;
import io.particle.android.sdk.devicesetup.ui.ConnectingFragment;
import io.particle.android.sdk.devicesetup.ui.ConnectingProcessWorkerTask;
import io.particle.android.sdk.devicesetup.ui.DiscoverDeviceActivity;
import io.particle.android.sdk.devicesetup.ui.DiscoverDeviceFragment;
import io.particle.android.sdk.devicesetup.ui.GetReadyActivity;
import io.particle.android.sdk.devicesetup.ui.GetReadyFragment;
import io.particle.android.sdk.devicesetup.ui.ManualNetworkEntryActivity;
import io.particle.android.sdk.devicesetup.ui.ManualNetworkEntryFragment;
import io.particle.android.sdk.devicesetup.ui.PasswordEntryActivity;
import io.particle.android.sdk.devicesetup.ui.PasswordEntryFragment;
import io.particle.android.sdk.devicesetup.ui.SelectNetworkActivity;
import io.particle.android.sdk.devicesetup.ui.SelectNetworkFragment;
import io.particle.android.sdk.devicesetup.ui.SuccessActivity;
import io.particle.android.sdk.devicesetup.ui.SuccessFragment;

@PerActivity
@Subcomponent(modules = {ApModule.class})
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

    void inject(ConnectingProcessWorkerTask connectingProcessWorkerTask);

    void inject(LoginFragment loginFragment);

    void inject(PasswordResetFragment passwordResetFragment);

    void inject(ConnectingFragment connectingFragment);

    void inject(DiscoverDeviceFragment discoverDeviceFragment);

    void inject(GetReadyFragment getReadyFragment);

    void inject(ManualNetworkEntryFragment manualNetworkEntryFragment);

    void inject(PasswordEntryFragment passwordEntryFragment);

    void inject(SelectNetworkFragment selectNetworkFragment);

    void inject(SuccessFragment successFragment);

    @Subcomponent.Builder
    interface Builder {
        Builder apModule(ApModule module);

        ActivityInjectorComponent build();
    }

}
