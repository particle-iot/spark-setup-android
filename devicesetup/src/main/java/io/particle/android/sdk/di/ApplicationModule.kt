package io.particle.android.sdk.di

import android.app.Application
import android.content.Context
import android.support.annotation.RestrictTo
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ApplicationModule @RestrictTo(RestrictTo.Scope.LIBRARY)
constructor(private val application: Application) {

    @Singleton
    @Provides
    fun providesApplication(): Application {
        return application
    }

    @Singleton
    @Provides
    fun providesContext(): Context {
        return application
    }

}
