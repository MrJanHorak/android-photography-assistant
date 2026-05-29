package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A named packing kit built from the user's saved gear inventory. */
@Entity(tableName = "gear_kits")
data class GearKitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)
