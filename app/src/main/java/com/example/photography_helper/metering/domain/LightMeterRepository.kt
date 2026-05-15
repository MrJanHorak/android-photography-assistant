package com.example.photography_helper.metering.domain

interface LightMeterRepository {
    fun startMetering()
    fun stopMetering()
    fun setCalibrationOffset(offsetEv: Float)
    fun getEvStream(): kotlinx.coroutines.flow.Flow<Float>
}
