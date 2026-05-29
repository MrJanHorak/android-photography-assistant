package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-owned memory card that can optionally be linked to a saved body or accessory. */
@Entity(
    tableName = "gear_memory_cards",
    foreignKeys = [
        ForeignKey(
            entity = GearItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedGearItemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("linkedGearItemId")],
)
data class GearMemoryCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val linkedGearItemId: Long?,
    val label: String,
    val cardType: String,
    val capacityGb: Int?,
    val speedLabel: String,
    val status: String,
    val lastFormattedText: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)
