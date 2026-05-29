package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-defined scene preset pairing a subject-motion profile with a stabilization mode. */
@Entity(tableName = "scene_presets")
data class ScenePresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subjectMotionProfile: String,
    val stabilizationMode: String,
)
