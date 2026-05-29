package com.janhorak.shutterdeck.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GearKitDao {
    @Query("SELECT * FROM gear_kits ORDER BY createdAt DESC")
    fun observeKits(): Flow<List<GearKitEntity>>

    @Query("SELECT * FROM gear_kit_items ORDER BY packed ASC, id ASC")
    fun observeKitItems(): Flow<List<GearKitItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKit(kit: GearKitEntity): Long

    @Delete
    suspend fun deleteKit(kit: GearKitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKitItem(item: GearKitItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKitItems(items: List<GearKitItemEntity>)

    @Query("DELETE FROM gear_kit_items WHERE kitId = :kitId")
    suspend fun deleteKitItemsForKit(kitId: Long)

    @Transaction
    suspend fun replaceKit(
        kit: GearKitEntity,
        gearItemIds: List<Long>,
    ): Long {
        val kitId = if (kit.id == 0L) {
            upsertKit(kit)
        } else {
            upsertKit(kit)
            kit.id
        }
        deleteKitItemsForKit(kitId)
        val distinctIds = gearItemIds.distinct()
        if (distinctIds.isNotEmpty()) {
            insertKitItems(
                distinctIds.map { gearItemId ->
                    GearKitItemEntity(
                        kitId = kitId,
                        gearItemId = gearItemId,
                    )
                },
            )
        }
        return kitId
    }
}
