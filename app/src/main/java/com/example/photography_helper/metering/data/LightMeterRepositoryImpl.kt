package com.example.photography_helper.metering.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.photography_helper.metering.domain.LightMeterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log2
import kotlin.math.max

@Singleton
class LightMeterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LightMeterRepository, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val _evStream = MutableStateFlow(0f)
    private var calibrationOffset = 0f

    override fun startMetering() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun stopMetering() {
        sensorManager.unregisterListener(this)
    }

    override fun setCalibrationOffset(offsetEv: Float) {
        // Clamp to +/- 3.0 EV
        calibrationOffset = offsetEv.coerceIn(-3.0f, 3.0f)
    }

    override fun getEvStream(): Flow<Float> = _evStream.asStateFlow()

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = max(event.values[0], 0.0001f) // Prevent log(0)
            // EV = log2(lux / 2.5)
            val baseEv = log2(lux / 2.5f)
            _evStream.value = baseEv + calibrationOffset
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for light sensor EV calculation
    }
}