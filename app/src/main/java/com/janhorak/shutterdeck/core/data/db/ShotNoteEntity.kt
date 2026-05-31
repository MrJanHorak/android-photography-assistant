package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shot_notes")
data class ShotNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val shotLabel: String = "",
    val noteText: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis,
)
