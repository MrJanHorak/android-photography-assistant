package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-owned screw-in or system filter tracked separately from the lens inventory. */
@Entity(tableName = "gear_filters")
data class GearFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val filterType: String,
    val threadSizeText: String,
    val strengthText: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)
