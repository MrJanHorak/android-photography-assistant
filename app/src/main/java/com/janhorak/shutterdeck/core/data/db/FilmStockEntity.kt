package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "film_stocks")
data class FilmStockEntity(
    @PrimaryKey val id: String,
    val brand: String,
    val name: String,
    val format: String,
    val stockType: String,
    val iso: Int,
    val reciprocityExponent: Double?,
    val reciprocityStartsAtSeconds: Double?,
    val processingType: String,
    val developerNotes: String,
    val description: String,
    val maxPushStops: Int?,
    val maxPullStops: Int?,
    val isBuiltIn: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val displayName: String
        get() = listOf(brand.trim(), name.trim()).filter { it.isNotBlank() }.joinToString(" ")
}
