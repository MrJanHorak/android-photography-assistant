package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved scouting location for trip planning. Coordinates are optional. */
@Entity(tableName = "scout_locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val referencePhotoUri: String = "",
    val notes: String,
    val bestTime: String,
    val createdAt: Long = System.currentTimeMillis(),
)
