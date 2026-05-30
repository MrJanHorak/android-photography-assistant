package com.janhorak.shutterdeck.utilities.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HistogramZebraAnalysisTest {

    @Test
    fun analyzeHistogramZebraBuildsExpectedBinsAndMetrics() {
        val analysis = analyzeHistogramZebra(
            luminance = byteArrayOf(
                0, 0, 64, 64,
                128.toByte(), 128.toByte(), 255.toByte(), 255.toByte(),
            ),
            frameWidth = 4,
            frameHeight = 2,
            histogramBinCount = 4,
            zebraThreshold = 250,
            zebraColumns = 2,
            zebraRows = 1,
            zebraActivationFraction = 0.5f,
        )

        assertEquals(listOf(2, 2, 2, 2), analysis.histogramCounts)
        assertEquals(1, analysis.activeZebraCells.size)
        assertEquals(1, analysis.activeZebraCells.single().column)
        assertEquals(0, analysis.activeZebraCells.single().row)
        assertEquals(0.5f, analysis.activeZebraCells.single().highlightedFraction, EPSILON)
        assertEquals(0.4382f, analysis.averageLuminanceRatio, 0.001f)
        assertEquals(0.25f, analysis.highlightRatio, EPSILON)
    }

    @Test
    fun analyzeHistogramZebraUsesActivationFractionPerCell() {
        val analysis = analyzeHistogramZebra(
            luminance = byteArrayOf(
                255.toByte(), 10, 255.toByte(), 10,
                10, 10, 255.toByte(), 255.toByte(),
                255.toByte(), 255.toByte(), 10, 10,
                255.toByte(), 255.toByte(), 10, 10,
            ),
            frameWidth = 4,
            frameHeight = 4,
            histogramBinCount = 4,
            zebraThreshold = 250,
            zebraColumns = 2,
            zebraRows = 2,
            zebraActivationFraction = 0.75f,
        )

        assertEquals(
            listOf(
                ZebraCell(column = 1, row = 0, highlightedFraction = 0.75f),
                ZebraCell(column = 0, row = 1, highlightedFraction = 1f),
            ),
            analysis.activeZebraCells,
        )
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
