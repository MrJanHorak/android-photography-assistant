package com.example.photography_helper.metering.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photography_helper.metering.domain.LightMeterRepository
import com.example.photography_helper.metering.domain.LightMeterState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class LightMeterViewModel @Inject constructor(
    private val repository: LightMeterRepository
) : ViewModel() {

    val meteringState: StateFlow<LightMeterState> = repository.getMeteringStateStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LightMeterState(
                sensorAvailable = false,
                isMetering = false,
            )
        )

    fun startMetering() {
        repository.startMetering()
    }

    fun stopMetering() {
        repository.stopMetering()
    }

    fun setCalibrationOffset(offset: Float) {
        repository.setCalibrationOffset(offset)
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
}