package com.janhorak.shutterdeck.film.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.FilmFrameEntity
import com.janhorak.shutterdeck.core.data.db.FilmRollDao
import com.janhorak.shutterdeck.core.data.db.FilmRollEntity
import com.janhorak.shutterdeck.film.domain.FILM_ROLL_STATUS_ACTIVE
import com.janhorak.shutterdeck.film.domain.FILM_ROLL_STATUS_FINISHED
import com.janhorak.shutterdeck.film.domain.FilmRollExportBundle
import com.janhorak.shutterdeck.film.domain.FilmRollExportFrame
import com.janhorak.shutterdeck.film.domain.FilmRollExportMetadata
import com.janhorak.shutterdeck.film.domain.buildFilmRollCsv
import com.janhorak.shutterdeck.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class FilmRollDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rollDao: FilmRollDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val rollId: Long = savedStateHandle.get<Long>(Routes.FILM_ROLL_ID_ARG) ?: 0L

    private val _frameStatus = MutableStateFlow<String?>(null)
    val frameStatus: StateFlow<String?> = _frameStatus.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    val roll: StateFlow<FilmRollEntity?> = rollDao.observeRoll(rollId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val frames: StateFlow<List<FilmFrameEntity>> = rollDao.observeFramesForRoll(rollId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveFrame(
        existing: FilmFrameEntity?,
        frameNumber: Int?,
        exposureSequence: Int?,
        apertureText: String,
        shutterSpeedText: String,
        focalLengthText: String,
        capturedAtText: String,
        latitude: Double?,
        longitude: Double?,
        notes: String,
    ): Boolean {
        val currentRoll = roll.value
        if (currentRoll == null) {
            _frameStatus.value = "This roll is no longer available."
            return false
        }

        if (currentRoll.status == FILM_ROLL_STATUS_FINISHED && existing == null) {
            _frameStatus.value = "Reopen the roll before logging another frame."
            return false
        }

        if (frameNumber == null || frameNumber <= 0) {
            _frameStatus.value = "Frame number must be greater than 0."
            return false
        }
        if (exposureSequence == null || exposureSequence <= 0) {
            _frameStatus.value = "Exposure index must be greater than 0."
            return false
        }

        val trimmedApertureText = apertureText.trim()
        val trimmedShutterSpeedText = shutterSpeedText.trim()
        val trimmedFocalLengthText = focalLengthText.trim()
        val trimmedCapturedAtText = capturedAtText.trim()

        if (
            trimmedApertureText.isEmpty() ||
            trimmedShutterSpeedText.isEmpty() ||
            trimmedFocalLengthText.isEmpty() ||
            trimmedCapturedAtText.isEmpty()
        ) {
            _frameStatus.value = "Aperture, shutter, focal length and time are required for each frame."
            return false
        }

        val conflictExists = frames.value.any { frame ->
            frame.id != existing?.id &&
                frame.frameNumber == frameNumber &&
                frame.exposureSequence == exposureSequence
        }
        if (conflictExists) {
            _frameStatus.value = "That frame and exposure slot is already logged."
            return false
        }

        viewModelScope.launch {
            rollDao.upsertFrame(
                FilmFrameEntity(
                    id = existing?.id ?: 0,
                    rollId = currentRoll.id,
                    frameNumber = frameNumber,
                    exposureSequence = exposureSequence,
                    apertureText = trimmedApertureText,
                    shutterSpeedText = trimmedShutterSpeedText,
                    focalLengthText = trimmedFocalLengthText,
                    capturedAtText = trimmedCapturedAtText,
                    latitude = latitude,
                    longitude = longitude,
                    notes = notes.trim(),
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
            _frameStatus.value = if (existing == null) {
                "Logged frame $frameNumber."
            } else {
                "Updated frame $frameNumber."
            }
        }
        return true
    }

    fun deleteFrame(frame: FilmFrameEntity) {
        viewModelScope.launch {
            rollDao.deleteFrame(frame)
            _frameStatus.value = "Deleted ${frame.displayLabel}."
        }
    }

    fun finishRoll() {
        val currentRoll = roll.value ?: return
        if (currentRoll.status == FILM_ROLL_STATUS_FINISHED) return
        viewModelScope.launch {
            rollDao.upsertRoll(
                currentRoll.copy(
                    status = FILM_ROLL_STATUS_FINISHED,
                    finishedOnText = currentRoll.finishedOnText.ifBlank { LocalDate.now().toString() },
                ),
            )
            _frameStatus.value = "Marked ${currentRoll.displayTitle} as finished."
        }
    }

    fun reopenRoll() {
        val currentRoll = roll.value ?: return
        if (currentRoll.status == FILM_ROLL_STATUS_ACTIVE) return
        viewModelScope.launch {
            rollDao.upsertRoll(
                currentRoll.copy(
                    status = FILM_ROLL_STATUS_ACTIVE,
                    finishedOnText = "",
                ),
            )
            _frameStatus.value = "Reopened ${currentRoll.displayTitle}."
        }
    }

    fun exportRollCsv(destination: Uri) {
        val currentRoll = roll.value
        if (currentRoll == null) {
            _exportStatus.value = "This roll is no longer available."
            return
        }
        val currentFrames = frames.value

        viewModelScope.launch {
            runCatching {
                context.contentResolver.openOutputStream(destination)?.bufferedWriter()?.use { writer ->
                    writer.write(
                        buildFilmRollCsv(
                            FilmRollExportBundle(
                                metadata = FilmRollExportMetadata(
                                    title = currentRoll.title,
                                    status = currentRoll.status,
                                    stockDisplayName = currentRoll.stockDisplayName,
                                    stockFormat = currentRoll.stockFormat,
                                    stockType = currentRoll.stockType,
                                    stockProcessingType = currentRoll.stockProcessingType,
                                    stockBaseIso = currentRoll.stockBaseIso,
                                    stockReciprocityExponent = currentRoll.stockReciprocityExponent,
                                    stockReciprocityStartsAtSeconds = currentRoll.stockReciprocityStartsAtSeconds,
                                    exposureIndex = currentRoll.exposureIndex,
                                    totalFrames = currentRoll.totalFrames,
                                    cameraLabel = currentRoll.cameraLabel,
                                    lensLabel = currentRoll.lensLabel,
                                    startedOnText = currentRoll.startedOnText,
                                    finishedOnText = currentRoll.finishedOnText,
                                    notes = currentRoll.notes,
                                    generatedAt = LocalDateTime.now(),
                                ),
                                frames = currentFrames.map { frame ->
                                    FilmRollExportFrame(
                                        frameNumber = frame.frameNumber,
                                        exposureSequence = frame.exposureSequence,
                                        apertureText = frame.apertureText,
                                        shutterSpeedText = frame.shutterSpeedText,
                                        focalLengthText = frame.focalLengthText,
                                        capturedAtText = frame.capturedAtText,
                                        latitude = frame.latitude,
                                        longitude = frame.longitude,
                                        notes = frame.notes,
                                    )
                                },
                            ),
                        ),
                    )
                } ?: error("Unable to open the selected destination.")
            }.onSuccess {
                _exportStatus.value = "Saved the roll log CSV."
            }.onFailure { throwable ->
                _exportStatus.value = throwable.message ?: "Unable to save the roll log CSV."
            }
        }
    }

    fun clearFrameStatus() {
        _frameStatus.value = null
    }

    fun clearExportStatus() {
        _exportStatus.value = null
    }
}
