package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A maintenance or firmware log entry attached to a specific gear item. */
@Entity(
    tableName = "gear_maintenance_entries",
    foreignKeys = [
        ForeignKey(
            entity = GearItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["gearItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gearItemId")],
)
data class GearMaintenanceEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gearItemId: Long,
    val eventType: String,
    val dateText: String,
    val shutterCount: Long?,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)
