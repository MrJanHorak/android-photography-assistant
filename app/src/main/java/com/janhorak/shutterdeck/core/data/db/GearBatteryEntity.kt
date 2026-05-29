package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-owned camera battery or power pack that can optionally be linked to saved gear. */
@Entity(
    tableName = "gear_batteries",
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
data class GearBatteryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val linkedGearItemId: Long?,
    val label: String,
    val capacityMah: Int?,
    val healthPercent: Int?,
    val chargePercent: Int?,
    val status: String,
    val lastChargedText: String,
    val lastCheckedText: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)
