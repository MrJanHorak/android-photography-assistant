package com.janhorak.shutterdeck.gear.presentation

data class GearItemEditState(
    val id: Long,
    val category: String,
    val brand: String,
    val model: String,
    val catalogId: String?,
    val filterThreadSizeText: String,
    val conditionLabel: String,
    val storageLocation: String,
    val purchaseSource: String,
    val referencePhotoUri: String,
    val serialNumber: String,
    val purchaseDateText: String,
    val purchasePrice: Double?,
    val currentValue: Double?,
    val weightGrams: Double?,
    val notes: String,
)
