package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmStockDao {
    @Query(
        """
        SELECT * FROM film_stocks
        ORDER BY isBuiltIn DESC, brand COLLATE NOCASE ASC, name COLLATE NOCASE ASC, iso ASC, format COLLATE NOCASE ASC
        """,
    )
    fun observeAll(): Flow<List<FilmStockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stock: FilmStockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stocks: List<FilmStockEntity>)

    @Delete
    suspend fun delete(stock: FilmStockEntity)
}
