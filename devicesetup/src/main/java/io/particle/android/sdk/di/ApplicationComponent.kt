package io.particle.android.sdk.di

import android.app.Application
import android.content.Context
import android.support.annotation.RestrictTo
import com.google.gson.Gson
import dagger.Component
import io.particle.android.sdk.cloud.ParticleCloud
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, CloudModule::class])
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface ApplicationComponent {

    val application: Application

    val context: Context

    val particleCloud: ParticleCloud

    val gson: Gson
    fun activityComponentBuilder(): ActivityInjectorComponent.Builder
}
