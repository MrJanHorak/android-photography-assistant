package com.example.photography_helper.metering.di

import com.example.photography_helper.metering.data.LightMeterRepositoryImpl
import com.example.photography_helper.metering.domain.LightMeterRepository
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