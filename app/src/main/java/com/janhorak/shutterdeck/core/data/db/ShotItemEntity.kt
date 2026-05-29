package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A single planned shot belonging to a [ShootEntity]. Deleted with its parent shoot. */
@Entity(
    tableName = "shot_items",
    foreignKeys = [
        ForeignKey(
            entity = ShootEntity::class,
            parentColumns = ["id"],
            childColumns = ["shootId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("shootId")],
)
data class ShotItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shootId: Long,
    val description: String,
    val done: Boolean = false,
    val sortOrder: Long = System.currentTimeMillis(),
)
