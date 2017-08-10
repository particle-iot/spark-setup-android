package io.particle.android.sdk.di;

import android.content.Context;
import android.support.annotation.RestrictTo;

import dagger.Module;
import dagger.Provides;
import io.particle.android.sdk.devicesetup.ApConnector;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.WifiFacade;

@Module
@RestrictTo({RestrictTo.Scope.LIBRARY})
public class ApModule {

    @Provides
    SoftAPConfigRemover providesSoftApConfigRemover(Context context, WifiFacade wifiFacade) {
        return new SoftAPConfigRemover(context, wifiFacade);
    }

    @Provides
    ApConnector providesApConnector(Context context, SoftAPConfigRemover configRemover, WifiFacade wifiFacade) {
        return new ApConnector(context, configRemover, wifiFacade);
    }

    @Provides
    WifiFacade providesWifiFacade(Context context) {
        return WifiFacade.get(context);
    }
}
