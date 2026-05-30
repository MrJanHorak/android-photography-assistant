package com.janhorak.shutterdeck.planner.domain

import com.janhorak.shutterdeck.core.data.db.ShotItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotOrderingTest {

    @Test
    fun moveUp_reordersWithinMatchingDoneGroup() {
        val shots = listOf(
            shot(id = 1, done = false, sortOrder = 10),
            shot(id = 2, done = false, sortOrder = 20),
            shot(id = 3, done = true, sortOrder = 30),
        )

        val result = reorderShotsWithinStatusGroup(
            shots = shots,
            shotId = 2,
            moveBy = -1,
        )

        assertEquals(listOf(2L, 1L), result.map { it.id })
        assertEquals(listOf(0L, 1L), result.map { it.sortOrder })
    }

    @Test
    fun moveDown_doesNotCrossDoneBoundary() {
        val shots = listOf(
            shot(id = 1, done = false, sortOrder = 10),
            shot(id = 2, done = false, sortOrder = 20),
            shot(id = 3, done = true, sortOrder = 30),
        )

        val result = reorderShotsWithinStatusGroup(
            shots = shots,
            shotId = 2,
            moveBy = 1,
        )

        assertTrue(result.isEmpty())
    }

    private fun shot(
        id: Long,
        done: Boolean,
        sortOrder: Long,
    ) = ShotItemEntity(
        id = id,
        shootId = 99,
        description = "Shot $id",
        done = done,
        sortOrder = sortOrder,
    )
}
