package com.janhorak.shutterdeck.planner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.LocationDao
import com.janhorak.shutterdeck.core.data.db.LocationEntity
import com.janhorak.shutterdeck.core.storage.ReferencePhotoGrantManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val dao: LocationDao,
    private val referencePhotoGrantManager: ReferencePhotoGrantManager,
) : ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    val locations: StateFlow<List<LocationEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(
        id: Long,
        name: String,
        latitude: Double?,
        longitude: Double?,
        notes: String,
        bestTime: String,
        referencePhotoUri: String,
        onSuccess: () -> Unit = {},
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = locations.value.firstOrNull { it.id == id }
            if (
                !referencePhotoGrantManager.updateLocationReferencePhotoGrant(
                    locationId = id,
                    previousUriString = existing?.referencePhotoUri.orEmpty(),
                    nextUriString = referencePhotoUri,
                )
            ) {
                _status.value = "Couldn't save access to the selected reference photo. Choose it again."
                return@launch
            }
            dao.upsert(
                LocationEntity(
                    id = id,
                    name = trimmed,
                    latitude = latitude,
                    longitude = longitude,
                    referencePhotoUri = referencePhotoUri.trim(),
                    notes = notes.trim(),
                    bestTime = bestTime.trim(),
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
            _status.value = null
            onSuccess()
        }
    }

    fun delete(location: LocationEntity) {
        viewModelScope.launch {
            referencePhotoGrantManager.releaseLocationReferencePhotoGrant(
                uriString = location.referencePhotoUri,
                excludingLocationId = location.id,
            )
            dao.delete(location)
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
