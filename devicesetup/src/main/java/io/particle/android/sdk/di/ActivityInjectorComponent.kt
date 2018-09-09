package io.particle.android.sdk.di

import android.support.annotation.RestrictTo
import dagger.Subcomponent
import io.particle.android.sdk.accountsetup.LoginFragment
import io.particle.android.sdk.accountsetup.PasswordResetFragment
import io.particle.android.sdk.accountsetup.TwoFactorFragment
import io.particle.android.sdk.devicesetup.ui.*

@PerActivity
@Subcomponent(modules = [ApModule::class])
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface ActivityInjectorComponent {

    fun inject(connectToApFragment: ConnectToApFragment)

    fun inject(connectingProcessWorkerTask: ConnectingProcessWorkerTask)

    fun inject(loginFragment: LoginFragment)

    fun inject(passwordResetFragment: PasswordResetFragment)

    fun inject(connectingFragment: ConnectingFragment)

    fun inject(discoverDeviceFragment: DiscoverDeviceFragment)

    fun inject(getReadyFragment: GetReadyFragment)

    fun inject(manualNetworkEntryFragment: ManualNetworkEntryFragment)

    fun inject(passwordEntryFragment: PasswordEntryFragment)

    fun inject(selectNetworkFragment: SelectNetworkFragment)

    fun inject(successFragment: SuccessFragment)

    fun inject(twoFactorFragment: TwoFactorFragment)

    @Subcomponent.Builder
    interface Builder {
        fun apModule(module: ApModule): Builder

        fun build(): ActivityInjectorComponent
    }

}
