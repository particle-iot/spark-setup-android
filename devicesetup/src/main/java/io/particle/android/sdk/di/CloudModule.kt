package io.particle.android.sdk.di

import android.support.annotation.RestrictTo
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import javax.inject.Singleton

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY)
class CloudModule {

    @Singleton
    @Provides
    fun providesParticleCloud(): ParticleCloud {
        return ParticleCloudSDK.getCloud()
    }

    @Singleton
    @Provides
    fun providesGson(): Gson {
        return Gson()
    }
}
