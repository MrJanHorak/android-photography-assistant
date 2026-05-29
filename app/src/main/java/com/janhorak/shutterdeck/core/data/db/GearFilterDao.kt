package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GearFilterDao {
    @Query("SELECT * FROM gear_filters ORDER BY filterType ASC, label ASC, createdAt DESC")
    fun observeAll(): Flow<List<GearFilterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GearFilterEntity): Long

    @Delete
    suspend fun delete(item: GearFilterEntity)
}
