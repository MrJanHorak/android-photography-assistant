package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "film_frames",
    foreignKeys = [
        ForeignKey(
            entity = FilmRollEntity::class,
            parentColumns = ["id"],
            childColumns = ["rollId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("rollId")],
)
data class FilmFrameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rollId: Long,
    val frameNumber: Int,
    val exposureSequence: Int = 1,
    val apertureText: String,
    val shutterSpeedText: String,
    val focalLengthText: String,
    val capturedAtText: String,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val displayLabel: String
        get() = if (exposureSequence <= 1) "Frame $frameNumber" else "Frame $frameNumber · Exp $exposureSequence"
}
