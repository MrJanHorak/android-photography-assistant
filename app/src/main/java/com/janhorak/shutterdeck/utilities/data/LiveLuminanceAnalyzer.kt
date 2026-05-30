package com.janhorak.shutterdeck.utilities.data

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.janhorak.shutterdeck.utilities.domain.HistogramZebraAnalysis
import com.janhorak.shutterdeck.utilities.domain.analyzeHistogramZebra
import java.util.concurrent.Executor
import kotlin.math.roundToInt
import android.os.SystemClock

data class LiveLuminanceAnalyzerConfig(
    val enabled: Boolean,
    val histogramBinCount: Int,
    val zebraThreshold: Int,
    val zebraColumns: Int,
    val zebraActivationFraction: Float,
    val minAnalysisIntervalMillis: Long,
)

class LiveLuminanceAnalyzer(
    private val callbackExecutor: Executor,
    private val onAnalysis: (HistogramZebraAnalysis) -> Unit,
) : ImageAnalysis.Analyzer {
    @Volatile
    private var config = LiveLuminanceAnalyzerConfig(
        enabled = true,
        histogramBinCount = 32,
        zebraThreshold = 250,
        zebraColumns = 20,
        zebraActivationFraction = 0.35f,
        minAnalysisIntervalMillis = 100L,
    )
    private var lastAnalysisAtMillis: Long = 0L

    fun updateConfig(newConfig: LiveLuminanceAnalyzerConfig) {
        config = newConfig
    }

    override fun analyze(image: ImageProxy) {
        try {
            val currentConfig = config
            if (!currentConfig.enabled) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastAnalysisAtMillis < currentConfig.minAnalysisIntervalMillis) return
            lastAnalysisAtMillis = now

            val frame = image.toCroppedLuminanceFrame()
            val zebraRows = (currentConfig.zebraColumns.toFloat() * frame.height.toFloat() / frame.width.toFloat())
                .roundToInt()
                .coerceAtLeast(1)
            val analysis = analyzeHistogramZebra(
                luminance = frame.luminance,
                frameWidth = frame.width,
                frameHeight = frame.height,
                histogramBinCount = currentConfig.histogramBinCount,
                zebraThreshold = currentConfig.zebraThreshold,
                zebraColumns = currentConfig.zebraColumns,
                zebraRows = zebraRows,
                zebraActivationFraction = currentConfig.zebraActivationFraction,
            )

            callbackExecutor.execute {
                onAnalysis(analysis)
            }
        } finally {
            image.close()
        }
    }
}

private data class CroppedLuminanceFrame(
    val width: Int,
    val height: Int,
    val luminance: ByteArray,
)

private fun ImageProxy.toCroppedLuminanceFrame(): CroppedLuminanceFrame {
    val crop = cropRect
    val width = crop.width()
    val height = crop.height()
    val plane = planes.first()
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val source = plane.buffer.duplicate()
    val luminance = ByteArray(width * height)

    for (row in 0 until height) {
        val rowOffset = row * width
        var sourceIndex = (crop.top + row) * rowStride + crop.left * pixelStride
        if (pixelStride == 1) {
            source.position(sourceIndex)
            source.get(luminance, rowOffset, width)
        } else {
            for (column in 0 until width) {
                luminance[rowOffset + column] = source.get(sourceIndex)
                sourceIndex += pixelStride
            }
        }
    }

    return CroppedLuminanceFrame(
        width = width,
        height = height,
        luminance = luminance,
    )
}
