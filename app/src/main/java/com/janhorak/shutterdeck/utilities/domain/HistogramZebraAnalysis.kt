package com.janhorak.shutterdeck.utilities.domain

data class ZebraCell(
    val column: Int,
    val row: Int,
    val highlightedFraction: Float,
)

data class HistogramZebraAnalysis(
    val frameWidth: Int,
    val frameHeight: Int,
    val histogramCounts: List<Int>,
    val zebraColumns: Int,
    val zebraRows: Int,
    val activeZebraCells: List<ZebraCell>,
    val averageLuminanceRatio: Float,
    val highlightRatio: Float,
)

fun analyzeHistogramZebra(
    luminance: ByteArray,
    frameWidth: Int,
    frameHeight: Int,
    histogramBinCount: Int = 32,
    zebraThreshold: Int = 250,
    zebraColumns: Int = 20,
    zebraRows: Int = 15,
    zebraActivationFraction: Float = 0.35f,
): HistogramZebraAnalysis {
    require(frameWidth > 0) { "frameWidth must be positive." }
    require(frameHeight > 0) { "frameHeight must be positive." }
    require(histogramBinCount > 0) { "histogramBinCount must be positive." }
    require(zebraColumns > 0) { "zebraColumns must be positive." }
    require(zebraRows > 0) { "zebraRows must be positive." }
    require(luminance.size == frameWidth * frameHeight) {
        "Luminance byte count must match frameWidth * frameHeight."
    }

    val clampedThreshold = zebraThreshold.coerceIn(0, 255)
    val histogram = IntArray(histogramBinCount)
    val highlightedPixelsPerCell = IntArray(zebraColumns * zebraRows)
    val totalPixelsPerCell = IntArray(zebraColumns * zebraRows)
    var highlightedPixels = 0
    var luminanceSum = 0L

    for (y in 0 until frameHeight) {
        for (x in 0 until frameWidth) {
            val index = y * frameWidth + x
            val luma = luminance[index].toInt() and 0xFF
            val histogramIndex = (luma * histogramBinCount / 256).coerceIn(0, histogramBinCount - 1)
            val cellColumn = x * zebraColumns / frameWidth
            val cellRow = y * zebraRows / frameHeight
            val cellIndex = cellRow * zebraColumns + cellColumn

            histogram[histogramIndex] += 1
            totalPixelsPerCell[cellIndex] += 1
            luminanceSum += luma

            if (luma >= clampedThreshold) {
                highlightedPixels += 1
                highlightedPixelsPerCell[cellIndex] += 1
            }
        }
    }

    val activeZebraCells = buildList {
        totalPixelsPerCell.forEachIndexed { cellIndex, totalPixels ->
            if (totalPixels == 0) return@forEachIndexed

            val highlightedFraction = highlightedPixelsPerCell[cellIndex].toFloat() / totalPixels.toFloat()
            if (highlightedFraction >= zebraActivationFraction) {
                add(
                    ZebraCell(
                        column = cellIndex % zebraColumns,
                        row = cellIndex / zebraColumns,
                        highlightedFraction = highlightedFraction,
                    ),
                )
            }
        }
    }

    val pixelCount = frameWidth * frameHeight
    return HistogramZebraAnalysis(
        frameWidth = frameWidth,
        frameHeight = frameHeight,
        histogramCounts = histogram.toList(),
        zebraColumns = zebraColumns,
        zebraRows = zebraRows,
        activeZebraCells = activeZebraCells,
        averageLuminanceRatio = luminanceSum.toFloat() / pixelCount.toFloat() / 255f,
        highlightRatio = highlightedPixels.toFloat() / pixelCount.toFloat(),
    )
}
