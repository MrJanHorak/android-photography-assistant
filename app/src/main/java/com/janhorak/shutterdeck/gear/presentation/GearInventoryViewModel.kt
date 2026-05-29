package com.janhorak.shutterdeck.gear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.GearItemDao
import com.janhorak.shutterdeck.core.data.db.GearItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GearInventoryViewModel @Inject constructor(
    private val dao: GearItemDao,
) : ViewModel() {

    val items: StateFlow<List<GearItemEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(
        id: Long,
        category: String,
        brand: String,
        model: String,
        serialNumber: String,
        purchaseDateText: String,
        purchasePrice: Double?,
        currentValue: Double?,
        weightGrams: Double?,
        notes: String,
    ) {
        val trimmedModel = model.trim()
        if (trimmedModel.isEmpty()) return
        viewModelScope.launch {
            dao.upsert(
                GearItemEntity(
                    id = id,
                    category = category.ifBlank { "Accessory" },
                    brand = brand.trim(),
                    model = trimmedModel,
                    serialNumber = serialNumber.trim(),
                    purchaseDateText = purchaseDateText.trim(),
                    purchasePrice = purchasePrice,
                    currentValue = currentValue,
                    weightGrams = weightGrams,
                    notes = notes.trim(),
                ),
            )
        }
    }

    fun delete(item: GearItemEntity) {
        viewModelScope.launch { dao.delete(item) }
    }
}
