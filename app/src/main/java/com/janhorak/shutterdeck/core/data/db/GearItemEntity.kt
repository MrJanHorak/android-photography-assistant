package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single user-owned gear item in the inventory.
 *
 * Strings are used for editable fields like category and purchase date so the feature can
 * evolve without Room converters or picker-specific UI.
 */
@Entity(tableName = "gear_items")
data class GearItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val brand: String,
    val model: String,
    val catalogId: String? = null,
    val serialNumber: String,
    val purchaseDateText: String,
    val purchasePrice: Double?,
    val currentValue: Double?,
    val weightGrams: Double?,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)
