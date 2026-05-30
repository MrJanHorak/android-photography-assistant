package com.janhorak.shutterdeck.planner.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.LocationDao
import com.janhorak.shutterdeck.core.data.db.LocationEntity
import com.janhorak.shutterdeck.core.data.db.ShootDao
import com.janhorak.shutterdeck.core.data.db.ShootEntity
import com.janhorak.shutterdeck.core.data.db.ShotItemEntity
import com.janhorak.shutterdeck.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShootHeaderUiState(
    val shoot: ShootEntity? = null,
    val location: LocationEntity? = null,
    val locationMissing: Boolean = false,
)

@HiltViewModel
class ShootDetailViewModel @Inject constructor(
    private val dao: ShootDao,
    private val locationDao: LocationDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val shootId: Long = savedStateHandle.get<Long>(Routes.SHOOT_ID_ARG) ?: 0L

    private val locationsFlow = locationDao.observeAll()

    val headerState: StateFlow<ShootHeaderUiState> = combine(
        dao.observeShoot(shootId),
        locationsFlow,
    ) { shoot, locations ->
        val location = shoot?.locationId?.let { locationId ->
            locations.firstOrNull { it.id == locationId }
        }
        ShootHeaderUiState(
            shoot = shoot,
            location = location,
            locationMissing = shoot?.locationId != null && location == null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShootHeaderUiState())

    val shots: StateFlow<List<ShotItemEntity>> = dao.observeShots(shootId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveShot(
        existing: ShotItemEntity?,
        description: String,
        gearNotes: String,
        notes: String,
    ) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val normalizedGearNotes = gearNotes.trim()
            val normalizedNotes = notes.trim()
            dao.upsertShot(
                existing?.copy(
                    description = trimmed,
                    gearNotes = normalizedGearNotes,
                    notes = normalizedNotes,
                ) ?: ShotItemEntity(
                    shootId = shootId,
                    description = trimmed,
                    gearNotes = normalizedGearNotes,
                    notes = normalizedNotes,
                ),
            )
        }
    }

    fun toggleDone(shot: ShotItemEntity) {
        viewModelScope.launch { dao.upsertShot(shot.copy(done = !shot.done)) }
    }

    fun delete(shot: ShotItemEntity) {
        viewModelScope.launch { dao.deleteShot(shot) }
    }
}
