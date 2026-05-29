package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single inventory item included in a packing kit.
 * `packed` lets the kit double as a pre-shoot checklist.
 */
@Entity(
    tableName = "gear_kit_items",
    foreignKeys = [
        ForeignKey(
            entity = GearKitEntity::class,
            parentColumns = ["id"],
            childColumns = ["kitId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GearItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["gearItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("kitId"),
        Index("gearItemId"),
        Index(value = ["kitId", "gearItemId"], unique = true),
    ],
)
data class GearKitItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kitId: Long,
    val gearItemId: Long,
    val packed: Boolean = false,
)
