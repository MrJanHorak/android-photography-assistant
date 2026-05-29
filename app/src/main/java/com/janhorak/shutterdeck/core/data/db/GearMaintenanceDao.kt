package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GearMaintenanceDao {
    @Query("SELECT * FROM gear_maintenance_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GearMaintenanceEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: GearMaintenanceEntryEntity): Long

    @Delete
    suspend fun delete(entry: GearMaintenanceEntryEntity)
}
