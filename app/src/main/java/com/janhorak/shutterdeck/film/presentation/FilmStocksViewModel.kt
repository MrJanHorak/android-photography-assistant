package com.janhorak.shutterdeck.film.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import com.janhorak.shutterdeck.film.data.FilmStockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FilmStocksViewModel @Inject constructor(
    private val repository: FilmStockRepository,
) : ViewModel() {

    val stocks: StateFlow<List<FilmStockEntity>> = repository.observeStocks()
        .map { stocks ->
            stocks.sortedWith(
                compareByDescending<FilmStockEntity> { stock -> stock.isBuiltIn }
                    .thenBy { stock -> stock.brand.lowercase(Locale.ROOT) }
                    .thenBy { stock -> stock.name.lowercase(Locale.ROOT) }
                    .thenBy { stock -> stock.iso }
                    .thenBy { stock -> stock.format.lowercase(Locale.ROOT) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(
        existing: FilmStockEntity?,
        brand: String,
        name: String,
        format: String,
        stockType: String,
        iso: Int?,
        reciprocityExponent: Double?,
        reciprocityStartsAtSeconds: Double?,
        processingType: String,
        maxPushStops: Int?,
        maxPullStops: Int?,
        developerNotes: String,
        description: String,
    ) {
        if (existing?.isBuiltIn == true) return

        val trimmedBrand = brand.trim()
        val trimmedName = name.trim()
        val trimmedFormat = format.trim()
        val trimmedStockType = stockType.trim()
        val trimmedProcessingType = processingType.trim()
        val trimmedDeveloperNotes = developerNotes.trim()
        val trimmedDescription = description.trim()
        val hasReciprocityInputs = reciprocityExponent != null || reciprocityStartsAtSeconds != null

        if (
            trimmedBrand.isEmpty() ||
            trimmedName.isEmpty() ||
            trimmedFormat.isEmpty() ||
            trimmedStockType.isEmpty() ||
            trimmedProcessingType.isEmpty() ||
            iso == null ||
            iso <= 0 ||
            (hasReciprocityInputs && (reciprocityExponent == null || reciprocityStartsAtSeconds == null)) ||
            (reciprocityExponent != null && reciprocityExponent <= 0.0) ||
            (reciprocityStartsAtSeconds != null && reciprocityStartsAtSeconds <= 0.0) ||
            (maxPushStops != null && maxPushStops < 0) ||
            (maxPullStops != null && maxPullStops < 0)
        ) {
            return
        }

        viewModelScope.launch {
            repository.saveCustomStock(
                FilmStockEntity(
                    id = existing?.id ?: "custom_${UUID.randomUUID()}",
                    brand = trimmedBrand,
                    name = trimmedName,
                    format = trimmedFormat,
                    stockType = trimmedStockType,
                    iso = iso,
                    reciprocityExponent = reciprocityExponent,
                    reciprocityStartsAtSeconds = reciprocityStartsAtSeconds,
                    processingType = trimmedProcessingType,
                    developerNotes = trimmedDeveloperNotes,
                    description = trimmedDescription,
                    maxPushStops = maxPushStops,
                    maxPullStops = maxPullStops,
                    isBuiltIn = false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
        }
    }

    fun delete(stock: FilmStockEntity) {
        if (stock.isBuiltIn) return
        viewModelScope.launch {
            repository.deleteCustomStock(stock)
        }
    }
}
