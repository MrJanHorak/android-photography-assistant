package com.janhorak.shutterdeck.utilities.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.janhorak.shutterdeck.utilities.domain.SpiritLevelReading
import com.janhorak.shutterdeck.utilities.domain.calculateSpiritLevelReading
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SpiritLevelSensorState(
    val sensorAvailable: Boolean,
    val active: Boolean,
    val reading: SpiritLevelReading? = null,
)

@Singleton
class SpiritLevelSensorRepository @Inject constructor(
    @ApplicationContext appContext: Context,
) : SensorEventListener {

    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val activeSensor = gravitySensor ?: accelerometerSensor
    private var filteredAccelerometerValues: FloatArray? = null

    private val _state = MutableStateFlow(
        SpiritLevelSensorState(
            sensorAvailable = activeSensor != null,
            active = false,
        ),
    )
    val state: StateFlow<SpiritLevelSensorState> = _state.asStateFlow()

    fun start() {
        val sensor = activeSensor
        if (sensor == null) {
            _state.value = SpiritLevelSensorState(sensorAvailable = false, active = false)
            return
        }

        val registered = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME,
        )

        _state.value = _state.value.copy(
            sensorAvailable = true,
            active = registered,
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        filteredAccelerometerValues = null
        _state.value = _state.value.copy(active = false)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val values = when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> event.values.copyOf()
            Sensor.TYPE_ACCELEROMETER -> applyLowPass(event.values)
            else -> return
        }

        _state.value = _state.value.copy(
            sensorAvailable = true,
            active = true,
            reading = calculateSpiritLevelReading(
                x = values[0],
                y = values[1],
                z = values[2],
            ),
        )
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    private fun applyLowPass(rawValues: FloatArray): FloatArray {
        val filtered = filteredAccelerometerValues ?: rawValues.copyOf().also {
            filteredAccelerometerValues = it
        }
        val alpha = 0.18f
        filtered.indices.forEach { index ->
            filtered[index] += alpha * (rawValues[index] - filtered[index])
        }
        return filtered.copyOf()
    }
}
