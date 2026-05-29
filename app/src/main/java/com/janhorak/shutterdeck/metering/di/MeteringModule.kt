package com.janhorak.shutterdeck.metering.di

import com.janhorak.shutterdeck.metering.data.LightMeterRepositoryImpl
import com.janhorak.shutterdeck.metering.domain.LightMeterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MeteringModule {

    @Binds
    @Singleton
    abstract fun bindLightMeterRepository(
        lightMeterRepositoryImpl: LightMeterRepositoryImpl
    ): LightMeterRepository
}