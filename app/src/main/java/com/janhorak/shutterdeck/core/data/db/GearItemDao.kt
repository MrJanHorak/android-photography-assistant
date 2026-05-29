package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GearItemDao {
    @Query("SELECT * FROM gear_items ORDER BY category ASC, brand ASC, model ASC, createdAt DESC")
    fun observeAll(): Flow<List<GearItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GearItemEntity): Long

    @Delete
    suspend fun delete(item: GearItemEntity)
}
