package com.janhorak.shutterdeck.planner.domain

import com.janhorak.shutterdeck.core.data.db.ShotItemEntity

fun reorderShotsWithinStatusGroup(
    shots: List<ShotItemEntity>,
    shotId: Long,
    moveBy: Int,
): List<ShotItemEntity> {
    val target = shots.firstOrNull { it.id == shotId } ?: return emptyList()
    val group = shots.filter { it.done == target.done }
    val currentIndex = group.indexOfFirst { it.id == shotId }
    if (currentIndex == -1) return emptyList()

    val destinationIndex = currentIndex + moveBy
    if (destinationIndex !in group.indices) return emptyList()

    val reordered = group.toMutableList().apply {
        add(destinationIndex, removeAt(currentIndex))
    }

    return reordered.mapIndexed { index, shot ->
        shot.copy(sortOrder = index.toLong())
    }
}
