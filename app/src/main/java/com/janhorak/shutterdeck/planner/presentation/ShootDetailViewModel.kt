package com.janhorak.shutterdeck.planner.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.ShootDao
import com.janhorak.shutterdeck.core.data.db.ShootEntity
import com.janhorak.shutterdeck.core.data.db.ShotItemEntity
import com.janhorak.shutterdeck.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShootDetailViewModel @Inject constructor(
    private val dao: ShootDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val shootId: Long = savedStateHandle.get<Long>(Routes.SHOOT_ID_ARG) ?: 0L

    val shoot: StateFlow<ShootEntity?> = dao.observeShoot(shootId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shots: StateFlow<List<ShotItemEntity>> = dao.observeShots(shootId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addShot(description: String) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.upsertShot(ShotItemEntity(shootId = shootId, description = trimmed))
        }
    }

    fun toggleDone(shot: ShotItemEntity) {
        viewModelScope.launch { dao.upsertShot(shot.copy(done = !shot.done)) }
    }

    fun delete(shot: ShotItemEntity) {
        viewModelScope.launch { dao.deleteShot(shot) }
    }
}
