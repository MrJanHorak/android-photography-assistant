package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LightingSetupDao {
    @Query("SELECT * FROM lighting_setups ORDER BY updatedAtMillis DESC, id DESC")
    fun observeSetups(): Flow<List<LightingSetupEntity>>

    @Query("SELECT * FROM lighting_setup_items ORDER BY setupId ASC, sortOrder ASC, id ASC")
    fun observeSetupItems(): Flow<List<LightingSetupItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetup(setup: LightingSetupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetupItems(items: List<LightingSetupItemEntity>)

    @Query("DELETE FROM lighting_setup_items WHERE setupId = :setupId")
    suspend fun deleteItemsForSetup(setupId: Long)

    @Delete
    suspend fun deleteSetup(setup: LightingSetupEntity)

    @Transaction
    suspend fun saveSetup(
        setup: LightingSetupEntity,
        items: List<LightingSetupItemEntity>,
    ): Long {
        val setupId = if (setup.id == 0L) {
            upsertSetup(setup)
        } else {
            upsertSetup(setup)
            setup.id
        }
        deleteItemsForSetup(setupId)
        if (items.isNotEmpty()) {
            insertSetupItems(
                items.mapIndexed { index, item ->
                    item.copy(
                        id = 0,
                        setupId = setupId,
                        sortOrder = index,
                    )
                },
            )
        }
        return setupId
    }
}
