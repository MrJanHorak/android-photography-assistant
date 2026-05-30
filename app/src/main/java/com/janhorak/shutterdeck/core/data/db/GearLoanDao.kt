package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GearLoanDao {
    @Query("SELECT * FROM gear_loans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GearLoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GearLoanEntity): Long

    @Delete
    suspend fun delete(item: GearLoanEntity)
}
