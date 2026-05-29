package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenePresetDao {
    @Query("SELECT * FROM scene_presets ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ScenePresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: ScenePresetEntity): Long

    @Delete
    suspend fun delete(preset: ScenePresetEntity)
}
