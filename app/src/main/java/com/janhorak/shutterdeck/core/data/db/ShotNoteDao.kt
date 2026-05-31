package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotNoteDao {

    @Query("SELECT * FROM shot_notes ORDER BY createdAtMillis DESC, id DESC")
    fun observeNotes(): Flow<List<ShotNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: ShotNoteEntity): Long

    @Delete
    suspend fun deleteNote(note: ShotNoteEntity)
}
