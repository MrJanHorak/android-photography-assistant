package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShootDao {
    @Query("SELECT * FROM shoots ORDER BY createdAt DESC")
    fun observeShoots(): Flow<List<ShootEntity>>

    @Query("SELECT * FROM shoots WHERE id = :shootId")
    fun observeShoot(shootId: Long): Flow<ShootEntity?>

    @Query("SELECT * FROM shot_items WHERE shootId = :shootId ORDER BY done ASC, sortOrder ASC")
    fun observeShots(shootId: Long): Flow<List<ShotItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoot(shoot: ShootEntity): Long

    @Delete
    suspend fun deleteShoot(shoot: ShootEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShot(shot: ShotItemEntity): Long

    @Delete
    suspend fun deleteShot(shot: ShotItemEntity)
}
