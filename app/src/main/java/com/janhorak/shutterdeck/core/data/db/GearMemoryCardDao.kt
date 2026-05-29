package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GearMemoryCardDao {
    @Query("SELECT * FROM gear_memory_cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GearMemoryCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GearMemoryCardEntity): Long

    @Delete
    suspend fun delete(item: GearMemoryCardEntity)
}
