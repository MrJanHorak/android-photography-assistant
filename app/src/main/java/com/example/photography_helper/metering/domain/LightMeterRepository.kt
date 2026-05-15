package com.example.photography_helper.metering.domain

import kotlinx.coroutines.flow.Flow

data class LightMeterState(
    val sensorAvailable: Boolean,
    val isMetering: Boolean,
    val ev: Float? = null,
    val lux: Float? = null,
    val calibrationOffsetEv: Float = 0f,
)

interface LightMeterRepository {
    fun startMetering()
    fun stopMetering()
    fun setCalibrationOffset(offsetEv: Float)
    fun getMeteringStateStream(): Flow<LightMeterState>
}
