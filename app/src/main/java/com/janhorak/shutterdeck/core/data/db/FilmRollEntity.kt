package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "film_rolls",
    foreignKeys = [
        ForeignKey(
            entity = FilmStockEntity::class,
            parentColumns = ["id"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("stockId")],
)
data class FilmRollEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockId: String?,
    val stockDisplayName: String,
    val stockFormat: String,
    val stockType: String,
    val stockProcessingType: String,
    val stockBaseIso: Int?,
    val stockReciprocityExponent: Double?,
    val stockReciprocityStartsAtSeconds: Double?,
    val exposureIndex: Int,
    val totalFrames: Int?,
    val title: String,
    val cameraLabel: String,
    val lensLabel: String,
    val status: String,
    val startedOnText: String,
    val finishedOnText: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val displayTitle: String
        get() = title.trim().ifBlank { stockDisplayName.trim().ifBlank { "Untitled roll" } }
}
