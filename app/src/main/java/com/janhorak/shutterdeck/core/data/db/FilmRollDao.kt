package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmRollDao {
    @Query(
        """
        SELECT * FROM film_rolls
        ORDER BY CASE WHEN status = 'Active' THEN 0 ELSE 1 END, createdAt DESC
        """,
    )
    fun observeRolls(): Flow<List<FilmRollEntity>>

    @Query("SELECT * FROM film_rolls WHERE id = :rollId LIMIT 1")
    fun observeRoll(rollId: Long): Flow<FilmRollEntity?>

    @Query(
        """
        SELECT * FROM film_frames
        WHERE rollId = :rollId
        ORDER BY frameNumber ASC, exposureSequence ASC, createdAt ASC
        """,
    )
    fun observeFramesForRoll(rollId: Long): Flow<List<FilmFrameEntity>>

    @Query(
        """
        SELECT * FROM film_frames
        ORDER BY rollId ASC, frameNumber ASC, exposureSequence ASC, createdAt ASC
        """,
    )
    fun observeAllFrames(): Flow<List<FilmFrameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoll(roll: FilmRollEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFrame(frame: FilmFrameEntity): Long

    @Delete
    suspend fun deleteRoll(roll: FilmRollEntity)

    @Delete
    suspend fun deleteFrame(frame: FilmFrameEntity)
}
