package com.janhorak.shutterdeck.film.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import com.janhorak.shutterdeck.core.data.db.FilmRollDao
import com.janhorak.shutterdeck.film.data.FilmStockRepository
import com.janhorak.shutterdeck.film.domain.FILM_ROLL_STATUS_ACTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FilmReferenceRollOption(
    val id: Long,
    val displayTitle: String,
    val stockId: String?,
    val stockDisplayName: String,
    val processingType: String,
    val baseIso: Int?,
    val exposureIndex: Int,
    val stockReciprocityExponent: Double?,
    val stockReciprocityStartsAtSeconds: Double?,
    val cameraLabel: String,
    val lensLabel: String,
    val maxPushStops: Int?,
    val maxPullStops: Int?,
    val developerNotes: String,
    val hasLiveStockDetails: Boolean,
)

data class FilmReferenceUiState(
    val stocks: List<FilmStockEntity> = emptyList(),
    val activeRolls: List<FilmReferenceRollOption> = emptyList(),
)

@HiltViewModel
class FilmReferenceViewModel @Inject constructor(
    filmRollDao: FilmRollDao,
    stockRepository: FilmStockRepository,
) : ViewModel() {
    val uiState: StateFlow<FilmReferenceUiState> = combine(
        stockRepository.observeStocks(),
        filmRollDao.observeRolls(),
    ) { stocks, rolls ->
        val sortedStocks = stocks.sortedWith(
            compareBy<FilmStockEntity>({ !it.isBuiltIn }, { it.displayName.lowercase() }, { it.iso }),
        )
        val stocksById = sortedStocks.associateBy { stock -> stock.id }
        val activeRolls = rolls
            .filter { roll -> roll.status == FILM_ROLL_STATUS_ACTIVE }
            .sortedByDescending { roll -> roll.createdAt }
            .map { roll ->
                val liveStock = roll.stockId?.let(stocksById::get)
                FilmReferenceRollOption(
                    id = roll.id,
                    displayTitle = roll.displayTitle,
                    stockId = roll.stockId,
                    stockDisplayName = roll.stockDisplayName,
                    processingType = roll.stockProcessingType,
                    baseIso = roll.stockBaseIso,
                    exposureIndex = roll.exposureIndex,
                    stockReciprocityExponent = roll.stockReciprocityExponent,
                    stockReciprocityStartsAtSeconds = roll.stockReciprocityStartsAtSeconds,
                    cameraLabel = roll.cameraLabel,
                    lensLabel = roll.lensLabel,
                    maxPushStops = liveStock?.maxPushStops,
                    maxPullStops = liveStock?.maxPullStops,
                    developerNotes = liveStock?.developerNotes.orEmpty(),
                    hasLiveStockDetails = liveStock != null,
                )
            }
        FilmReferenceUiState(
            stocks = sortedStocks,
            activeRolls = activeRolls,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FilmReferenceUiState(),
    )
}
