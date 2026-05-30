package com.janhorak.shutterdeck.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A loaned, borrowed or rented gear record, optionally linked to a saved inventory item. */
@Entity(
    tableName = "gear_loans",
    foreignKeys = [
        ForeignKey(
            entity = GearItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedGearItemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("linkedGearItemId")],
)
data class GearLoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val linkedGearItemId: Long?,
    val customItemLabel: String,
    val direction: String,
    val counterpartName: String,
    val status: String,
    val startDateText: String,
    val dueDateText: String,
    val returnedDateText: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)
