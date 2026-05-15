package com.example.photography_helper.metering.presentation

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photography_helper.metering.domain.LightMeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

@HiltViewModel
class LightMeterViewModel @Inject constructor(
    private val repository: LightMeterRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val cameraAvailable = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    private val selectedSource = MutableStateFlow(MeteringSource.AMBIENT_SENSOR)
    private val cameraPermissionGranted = MutableStateFlow(false)
    private val cameraReading = MutableStateFlow<ReflectiveMeterReading?>(null)

    val meteringState: StateFlow<MeteringUiState> = combine(
        repository.getMeteringStateStream(),
        selectedSource,
        cameraPermissionGranted,
        cameraReading,
    ) { ambientState, source, isCameraPermissionGranted, reflectiveReading ->
        MeteringUiState(
            selectedSource = source,
            sensorAvailable = ambientState.sensorAvailable,
            cameraAvailable = cameraAvailable,
            cameraPermissionGranted = isCameraPermissionGranted,
            isMetering = when (source) {
                MeteringSource.AMBIENT_SENSOR -> ambientState.isMetering
                MeteringSource.CAMERA_REFLECTIVE -> isCameraPermissionGranted && reflectiveReading != null
            },
            ev = when (source) {
                MeteringSource.AMBIENT_SENSOR -> ambientState.ev
                MeteringSource.CAMERA_REFLECTIVE -> {
                    reflectiveReading?.ev100?.plus(ambientState.calibrationOffsetEv)
                }
            },
            lux = if (source == MeteringSource.AMBIENT_SENSOR) ambientState.lux else null,
            calibrationOffsetEv = ambientState.calibrationOffsetEv,
            cameraReading = if (source == MeteringSource.CAMERA_REFLECTIVE) reflectiveReading else null,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MeteringUiState(
                sensorAvailable = false,
                cameraAvailable = cameraAvailable,
                isMetering = false,
            )
        )

    fun setMeteringSource(source: MeteringSource) {
        if (selectedSource.value == source) return

        selectedSource.value = source
        when (source) {
            MeteringSource.AMBIENT_SENSOR -> repository.startMetering()
            MeteringSource.CAMERA_REFLECTIVE -> {
                repository.stopMetering()
                onCameraMeteringStopped()
            }
        }
    }

    fun startMetering() {
        if (selectedSource.value == MeteringSource.AMBIENT_SENSOR) {
            repository.startMetering()
        }
    }

    fun stopMetering() {
        repository.stopMetering()
        onCameraMeteringStopped()
    }

    fun setCalibrationOffset(offset: Float) {
        repository.setCalibrationOffset(offset)
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        cameraPermissionGranted.value = granted
        if (!granted) {
            onCameraMeteringStopped()
        }
    }

    fun updateCameraReading(aperture: Float, shutterSeconds: Double, iso: Int) {
        val ev100 = calculateReflectiveEv(aperture, shutterSeconds, iso) ?: return
        val updatedReading = ReflectiveMeterReading(
            aperture = aperture,
            shutterSeconds = shutterSeconds,
            iso = iso,
            ev100 = ev100,
        )
        val currentReading = cameraReading.value

        if (currentReading != null &&
            currentReading.iso == updatedReading.iso &&
            abs(currentReading.aperture - updatedReading.aperture) < 0.05f &&
            abs(currentReading.shutterSeconds - updatedReading.shutterSeconds) < 0.0005 &&
            abs(currentReading.ev100 - updatedReading.ev100) < 0.05f
        ) {
            return
        }

        cameraReading.value = updatedReading
    }

    fun onCameraMeteringStopped() {
        cameraReading.value = null
    }

    /**
     * Calculates the required Shutter Speed (in seconds) for a given Aperture and ISO.
     * 2^EV = (N^2) / t * (ISO/100)  where N=Aperture, t=Shutter Speed
     * Therefore: t = (N^2 * (ISO/100)) / (2^EV)
     */
    fun calculateShutterSpeed(aperture: Float, iso: Int, currentEv: Float?): Double? {
        val ev = currentEv ?: return null
        val nSquared = aperture.toDouble().pow(2.0)
        val isoFactor = iso / 100.0
        val twoPowEv = 2.0.pow(ev.toDouble())
        return if (twoPowEv > 0.0) {
            (nSquared * isoFactor) / twoPowEv
        } else {
            null
        }
    }

    private fun calculateReflectiveEv(aperture: Float, shutterSeconds: Double, iso: Int): Float? {
        if (aperture <= 0f || shutterSeconds <= 0.0 || iso <= 0) return null

        val nSquared = aperture.toDouble().pow(2.0)
        val ev100 = log2((nSquared / shutterSeconds) * (100.0 / iso.toDouble()))
        return ev100.toFloat()
    }
}