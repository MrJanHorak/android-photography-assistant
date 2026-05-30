package com.janhorak.shutterdeck.utilities.presentation

import androidx.lifecycle.ViewModel
import com.janhorak.shutterdeck.utilities.data.SpiritLevelSensorRepository
import com.janhorak.shutterdeck.utilities.data.SpiritLevelSensorState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SpiritLevelViewModel @Inject constructor(
    private val repository: SpiritLevelSensorRepository,
) : ViewModel() {

    val state: StateFlow<SpiritLevelSensorState> = repository.state

    fun startLeveling() {
        repository.start()
    }

    fun stopLeveling() {
        repository.stop()
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}
