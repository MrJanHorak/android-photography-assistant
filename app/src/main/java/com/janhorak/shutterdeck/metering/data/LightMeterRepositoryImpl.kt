package com.janhorak.shutterdeck.metering.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.janhorak.shutterdeck.metering.domain.LightMeterRepository
import com.janhorak.shutterdeck.metering.domain.LightMeterState
import com.janhorak.shutterdeck.metering.domain.evFromLux
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class LightMeterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LightMeterRepository, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val _meteringState = MutableStateFlow(
        LightMeterState(
            sensorAvailable = lightSensor != null,
            isMetering = false,
        )
    )
    private var calibrationOffset = 0f
    private var lastLux = 0.0001f

    override fun startMetering() {
        val sensor = lightSensor
        if (sensor == null) {
            _meteringState.value = _meteringState.value.copy(
                sensorAvailable = false,
                isMetering = false,
            )
            return
        }

        val isRegistered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        _meteringState.value = _meteringState.value.copy(
            sensorAvailable = true,
            isMetering = isRegistered,
        )
    }

    override fun stopMetering() {
        sensorManager.unregisterListener(this)
        _meteringState.value = _meteringState.value.copy(isMetering = false)
    }

    override fun setCalibrationOffset(offsetEv: Float) {
        calibrationOffset = offsetEv.coerceIn(-3.0f, 3.0f)
        val currentState = _meteringState.value
        _meteringState.value = currentState.copy(
            calibrationOffsetEv = calibrationOffset,
            ev = currentState.lux?.let(::calculateEv),
        )
    }

    override fun getMeteringStateStream(): Flow<LightMeterState> = _meteringState.asStateFlow()

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            lastLux = max(event.values[0], 0.0001f)
            _meteringState.value = _meteringState.value.copy(
                sensorAvailable = true,
                isMetering = true,
                lux = lastLux,
                ev = calculateEv(lastLux),
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for this meter.
    }

    private fun calculateEv(lux: Float): Float = evFromLux(lux) + calibrationOffset
}