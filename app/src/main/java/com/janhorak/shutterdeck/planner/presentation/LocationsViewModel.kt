package com.janhorak.shutterdeck.planner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.LocationDao
import com.janhorak.shutterdeck.core.data.db.LocationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val dao: LocationDao,
) : ViewModel() {

    val locations: StateFlow<List<LocationEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(
        id: Long,
        name: String,
        latitude: Double?,
        longitude: Double?,
        notes: String,
        bestTime: String,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.upsert(
                LocationEntity(
                    id = id,
                    name = trimmed,
                    latitude = latitude,
                    longitude = longitude,
                    notes = notes.trim(),
                    bestTime = bestTime.trim(),
                ),
            )
        }
    }

    fun delete(location: LocationEntity) {
        viewModelScope.launch { dao.delete(location) }
    }
}
