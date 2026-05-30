package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM scout_locations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT COUNT(*) FROM scout_locations WHERE TRIM(referencePhotoUri) = :uriString AND id != :excludingId")
    suspend fun countByReferencePhotoUriExcludingId(uriString: String, excludingId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: LocationEntity): Long

    @Delete
    suspend fun delete(location: LocationEntity)
}
