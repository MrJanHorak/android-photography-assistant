package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lighting_setup_items",
    foreignKeys = [
        ForeignKey(
            entity = LightingSetupEntity::class,
            parentColumns = ["id"],
            childColumns = ["setupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["setupId"])],
)
data class LightingSetupItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setupId: Long,
    val itemType: String,
    val label: String,
    val xFraction: Float,
    val yFraction: Float,
    val sortOrder: Int,
)
