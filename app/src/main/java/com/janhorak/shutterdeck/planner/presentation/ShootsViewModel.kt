package com.janhorak.shutterdeck.planner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.ShootDao
import com.janhorak.shutterdeck.core.data.db.ShootEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShootsViewModel @Inject constructor(
    private val dao: ShootDao,
) : ViewModel() {

    val shoots: StateFlow<List<ShootEntity>> = dao.observeShoots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(id: Long, title: String, dateText: String, notes: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.upsertShoot(
                ShootEntity(
                    id = id,
                    title = trimmed,
                    dateText = dateText.trim(),
                    notes = notes.trim(),
                ),
            )
        }
    }

    fun delete(shoot: ShootEntity) {
        viewModelScope.launch { dao.deleteShoot(shoot) }
    }
}
