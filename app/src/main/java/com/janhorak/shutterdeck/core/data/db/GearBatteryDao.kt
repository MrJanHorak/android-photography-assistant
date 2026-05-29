package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GearBatteryDao {
    @Query("SELECT * FROM gear_batteries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GearBatteryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GearBatteryEntity): Long

    @Delete
    suspend fun delete(item: GearBatteryEntity)
}
