package com.janhorak.shutterdeck.planner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.LocationDao
import com.janhorak.shutterdeck.core.data.db.LocationEntity
import com.janhorak.shutterdeck.core.data.db.ShootDao
import com.janhorak.shutterdeck.core.data.db.ShootEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShootListItemUiState(
    val shoot: ShootEntity,
    val locationName: String? = null,
    val locationMissing: Boolean = false,
)

@HiltViewModel
class ShootsViewModel @Inject constructor(
    private val dao: ShootDao,
    private val locationDao: LocationDao,
) : ViewModel() {

    private val locationsFlow = locationDao.observeAll()

    val locations: StateFlow<List<LocationEntity>> = locationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoots: StateFlow<List<ShootListItemUiState>> = combine(
        dao.observeShoots(),
        locationsFlow,
    ) { shoots, locations ->
        val locationsById = locations.associateBy(LocationEntity::id)
        shoots.map { shoot ->
            val location = shoot.locationId?.let(locationsById::get)
            ShootListItemUiState(
                shoot = shoot,
                locationName = location?.name,
                locationMissing = shoot.locationId != null && location == null,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(id: Long, title: String, dateText: String, notes: String, locationId: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.upsertShoot(
                ShootEntity(
                    id = id,
                    title = trimmed,
                    dateText = dateText.trim(),
                    notes = notes.trim(),
                    locationId = locationId,
                ),
            )
        }
    }

    fun delete(shoot: ShootEntity) {
        viewModelScope.launch { dao.deleteShoot(shoot) }
    }
}
