package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A planned shoot that groups a checklist of shot items. */
@Entity(tableName = "shoots")
data class ShootEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateText: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)
