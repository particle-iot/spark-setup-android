package io.particle.android.sdk.di

import android.content.Context
import android.support.annotation.RestrictTo

import dagger.Module
import dagger.Provides
import io.particle.android.sdk.devicesetup.ApConnector
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsFactory
import io.particle.android.sdk.devicesetup.ui.DiscoverProcessWorker
import io.particle.android.sdk.utils.SoftAPConfigRemover
import io.particle.android.sdk.utils.WifiFacade

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ApModule {

    @Provides
    fun providesSoftApConfigRemover(context: Context, wifiFacade: WifiFacade): SoftAPConfigRemover {
        return SoftAPConfigRemover(context, wifiFacade)
    }

    @Provides
    fun providesWifiFacade(context: Context): WifiFacade {
        return WifiFacade[context]
    }

    @Provides
    fun providesDiscoverProcessWorker(): DiscoverProcessWorker {
        return DiscoverProcessWorker()
    }

    @Provides
    fun providesCommandClientFactory(): CommandClientFactory {
        return CommandClientFactory()
    }

    @Provides
    fun providesSetupStepsFactory(): SetupStepsFactory {
        return SetupStepsFactory()
    }

    @Provides
    fun providesApConnector(context: Context, softAPConfigRemover: SoftAPConfigRemover,
                            wifiFacade: WifiFacade): ApConnector {
        return ApConnector(context, softAPConfigRemover, wifiFacade)
    }
}
