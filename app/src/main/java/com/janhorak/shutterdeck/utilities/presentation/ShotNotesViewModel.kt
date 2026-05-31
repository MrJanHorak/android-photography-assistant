package com.janhorak.shutterdeck.utilities.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.ShotNoteDao
import com.janhorak.shutterdeck.core.data.db.ShotNoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ShotNotesViewModel @Inject constructor(
    private val shotNoteDao: ShotNoteDao,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    val notes: StateFlow<List<ShotNoteEntity>> = shotNoteDao.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveNote(
        existing: ShotNoteEntity?,
        shotLabel: String,
        noteText: String,
        latitude: Double?,
        longitude: Double?,
    ): Boolean {
        val trimmedNoteText = noteText.trim()
        if (trimmedNoteText.isEmpty()) {
            _status.value = "Enter a note before saving."
            return false
        }

        val now = System.currentTimeMillis()
        val hasLocation = latitude != null && longitude != null

        viewModelScope.launch {
            shotNoteDao.upsertNote(
                ShotNoteEntity(
                    id = existing?.id ?: 0L,
                    shotLabel = shotLabel.trim(),
                    noteText = trimmedNoteText,
                    latitude = latitude.takeIf { hasLocation },
                    longitude = longitude.takeIf { hasLocation },
                    createdAtMillis = existing?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                ),
            )
            _status.value = if (existing == null) {
                "Saved a shot note."
            } else {
                "Updated the shot note."
            }
        }
        return true
    }

    fun deleteNote(note: ShotNoteEntity) {
        viewModelScope.launch {
            shotNoteDao.deleteNote(note)
            _status.value = "Deleted the shot note."
        }
    }

    fun setStatus(message: String?) {
        _status.value = message
    }

    fun clearStatus() {
        _status.value = null
    }
}
