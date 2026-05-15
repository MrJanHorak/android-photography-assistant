package com.example.photography_helper.metering.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photography_helper.metering.domain.LightMeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt

@HiltViewModel
class LightMeterViewModel @Inject constructor(
    private val repository: LightMeterRepository
) : ViewModel() {

    // Expose the raw EV value
    val evState: StateFlow<Float> = repository.getEvStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
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
    fun calculateShutterSpeed(aperture: Float, iso: Int, currentEv: Float): Double {
        val nSquared = aperture.toDouble().pow(2.0)
        val isoFactor = iso / 100.0
        val twoPowEv = 2.0.pow(currentEv.toDouble())
        return if (twoPowEv > 0.0) {
            (nSquared * isoFactor) / twoPowEv
        } else {
            0.0
        }
    }
}