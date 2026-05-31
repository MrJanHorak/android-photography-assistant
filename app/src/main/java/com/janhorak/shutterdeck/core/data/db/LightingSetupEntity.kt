package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lighting_setups")
data class LightingSetupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
