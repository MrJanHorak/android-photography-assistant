package com.janhorak.shutterdeck.utilities.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CompositionOverlayGuidesTest {

    @Test
    fun ruleOfThirdsReturnsThirdsGrid() {
        val guides = compositionOverlayGuides(CompositionOverlayMode.RULE_OF_THIRDS)

        assertEquals(4, guides.size)
        assertEquals(1f / 3f, guides[0].startX, EPSILON)
        assertEquals(2f / 3f, guides[1].startX, EPSILON)
        assertEquals(1f / 3f, guides[2].startY, EPSILON)
        assertEquals(2f / 3f, guides[3].startY, EPSILON)
    }

    @Test
    fun goldenRatioReturnsGoldenSections() {
        val guides = compositionOverlayGuides(CompositionOverlayMode.GOLDEN_RATIO)

        assertEquals(4, guides.size)
        assertEquals(0.38196602f, guides[0].startX, EPSILON)
        assertEquals(0.618034f, guides[1].startX, EPSILON)
        assertEquals(0.38196602f, guides[2].startY, EPSILON)
        assertEquals(0.618034f, guides[3].startY, EPSILON)
    }

    @Test
    fun diagonalsReturnCornerToCornerX() {
        val guides = compositionOverlayGuides(CompositionOverlayMode.DIAGONALS)

        assertEquals(
            listOf(
                CompositionGuideLine(startX = 0f, startY = 0f, endX = 1f, endY = 1f),
                CompositionGuideLine(startX = 1f, startY = 0f, endX = 0f, endY = 1f),
            ),
            guides,
        )
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
