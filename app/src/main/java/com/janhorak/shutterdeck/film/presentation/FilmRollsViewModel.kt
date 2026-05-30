package com.janhorak.shutterdeck.film.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.FilmRollDao
import com.janhorak.shutterdeck.core.data.db.FilmRollEntity
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import com.janhorak.shutterdeck.film.data.FilmStockRepository
import com.janhorak.shutterdeck.film.domain.FILM_ROLL_STATUS_ACTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilmRollSummary(
    val roll: FilmRollEntity,
    val loggedFrameCount: Int,
    val loggedExposureCount: Int,
    val nextFrameNumber: Int,
    val atCapacity: Boolean,
)

@HiltViewModel
class FilmRollsViewModel @Inject constructor(
    private val rollDao: FilmRollDao,
    private val stockRepository: FilmStockRepository,
) : ViewModel() {

    private val _rollStatus = MutableStateFlow<String?>(null)
    val rollStatus: StateFlow<String?> = _rollStatus.asStateFlow()

    val stocks: StateFlow<List<FilmStockEntity>> = stockRepository.observeStocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rollSummaries: StateFlow<List<FilmRollSummary>> = combine(
        rollDao.observeRolls(),
        rollDao.observeAllFrames(),
    ) { rolls, frames ->
        val framesByRoll = frames.groupBy { frame -> frame.rollId }
        rolls.map { roll ->
            val rollFrames = framesByRoll[roll.id].orEmpty()
            val loggedFrameCount = rollFrames.map { frame -> frame.frameNumber }.distinct().size
            val loggedExposureCount = rollFrames.size
            val nextFrameNumber = (rollFrames.maxOfOrNull { frame -> frame.frameNumber } ?: 0) + 1
            val atCapacity = roll.totalFrames?.let { capacity -> loggedFrameCount >= capacity } ?: false
            FilmRollSummary(
                roll = roll,
                loggedFrameCount = loggedFrameCount,
                loggedExposureCount = loggedExposureCount,
                nextFrameNumber = nextFrameNumber,
                atCapacity = atCapacity,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRoll(
        existing: FilmRollEntity?,
        selectedStockId: String?,
        title: String,
        exposureIndex: Int?,
        totalFrames: Int?,
        cameraLabel: String,
        lensLabel: String,
        startedOnText: String,
        notes: String,
    ): Boolean {
        val selectedStock = stocks.value.firstOrNull { stock -> stock.id == selectedStockId }
        if (selectedStock == null) {
            _rollStatus.value = "Choose a film stock before saving the roll."
            return false
        }

        val trimmedCameraLabel = cameraLabel.trim()
        val trimmedLensLabel = lensLabel.trim()
        val trimmedStartedOnText = startedOnText.trim()

        if (exposureIndex == null || exposureIndex <= 0) {
            _rollStatus.value = "Exposure index must be greater than 0."
            return false
        }
        if (totalFrames != null && totalFrames <= 0) {
            _rollStatus.value = "Roll capacity must be greater than 0 when it is set."
            return false
        }
        if (trimmedCameraLabel.isEmpty() || trimmedLensLabel.isEmpty()) {
            _rollStatus.value = "Camera and lens are required for each roll."
            return false
        }
        if (trimmedStartedOnText.isEmpty()) {
            _rollStatus.value = "Enter a start date or note for the roll."
            return false
        }

        viewModelScope.launch {
            rollDao.upsertRoll(
                FilmRollEntity(
                    id = existing?.id ?: 0,
                    stockId = selectedStock.id,
                    stockDisplayName = selectedStock.displayName,
                    stockFormat = selectedStock.format,
                    stockType = selectedStock.stockType,
                    stockProcessingType = selectedStock.processingType,
                    stockBaseIso = selectedStock.iso,
                    stockReciprocityExponent = selectedStock.reciprocityExponent,
                    stockReciprocityStartsAtSeconds = selectedStock.reciprocityStartsAtSeconds,
                    exposureIndex = exposureIndex,
                    totalFrames = totalFrames,
                    title = title.trim(),
                    cameraLabel = trimmedCameraLabel,
                    lensLabel = trimmedLensLabel,
                    status = existing?.status ?: FILM_ROLL_STATUS_ACTIVE,
                    startedOnText = trimmedStartedOnText,
                    finishedOnText = existing?.finishedOnText.orEmpty(),
                    notes = notes.trim(),
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
            _rollStatus.value = if (existing == null) {
                "Started a new roll."
            } else {
                "Updated ${existing.displayTitle}."
            }
        }
        return true
    }

    fun deleteRoll(roll: FilmRollEntity) {
        viewModelScope.launch {
            rollDao.deleteRoll(roll)
            _rollStatus.value = "Deleted ${roll.displayTitle}."
        }
    }

    fun clearStatus() {
        _rollStatus.value = null
    }
}
