package com.janhorak.shutterdeck.utilities.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.janhorak.shutterdeck.utilities.domain.LightingSetupDraftItem
import com.janhorak.shutterdeck.utilities.domain.LightingSetupItemType
import com.janhorak.shutterdeck.utilities.domain.buildLightingSetupShareText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

data class LightingSetupSharePayload(
    val imageUri: Uri,
    val summaryText: String,
)

suspend fun prepareLightingSetupDiagramShare(
    context: Context,
    name: String,
    notes: String,
    items: List<LightingSetupDraftItem>,
    updatedAtMillis: Long,
): LightingSetupSharePayload = withContext(Dispatchers.IO) {
    val shareText = buildLightingSetupShareText(
        name = name,
        notes = notes,
        items = items,
        updatedAtMillis = updatedAtMillis,
    )
    val bitmap = renderLightingSetupBitmap(
        name = name,
        items = items,
        updatedAtMillis = updatedAtMillis,
    )
    val shareDirectory = File(context.cacheDir, "shared")
    if (!shareDirectory.exists() && !shareDirectory.mkdirs()) {
        throw IOException("Unable to create share cache directory.")
    }
    shareDirectory.listFiles()
        ?.filter { file -> file.name.startsWith("lighting-setup-") }
        ?.forEach { file -> file.delete() }

    val file = File(
        shareDirectory,
        "${slugifyLightingSetupName(name)}-$updatedAtMillis.png",
    )
    FileOutputStream(file).use { output ->
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            throw IOException("Unable to write the share image.")
        }
    }

    val imageUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    LightingSetupSharePayload(
        imageUri = imageUri,
        summaryText = shareText,
    )
}

private fun renderLightingSetupBitmap(
    name: String,
    items: List<LightingSetupDraftItem>,
    updatedAtMillis: Long,
): Bitmap {
    val width = 1600
    val height = 1200
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F3F5F8") }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 58f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textSize = 30f
    }
    canvas.drawText(name.ifBlank { "Untitled lighting setup" }, 100f, 88f, titlePaint)
    canvas.drawText("Updated ${formatLightingSetupShareTimestamp(updatedAtMillis)}", 100f, 132f, subtitlePaint)

    val stageRect = RectF(160f, 180f, 1440f, 1140f)
    val stageBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2E8F0") }
    val stageBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRoundRect(stageRect, 42f, 42f, stageBackgroundPaint)
    canvas.drawRoundRect(stageRect, 42f, 42f, stageBorderPaint)

    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        strokeWidth = 3f
    }
    repeat(2) { index ->
        val x = stageRect.left + stageRect.width() * ((index + 1) / 3f)
        val y = stageRect.top + stageRect.height() * ((index + 1) / 3f)
        canvas.drawLine(x, stageRect.top, x, stageRect.bottom, gridPaint)
        canvas.drawLine(stageRect.left, y, stageRect.right, y, gridPaint)
    }
    canvas.drawLine(
        stageRect.left + stageRect.width() / 2f,
        stageRect.top,
        stageRect.left + stageRect.width() / 2f,
        stageRect.bottom,
        gridPaint,
    )
    canvas.drawLine(
        stageRect.left,
        stageRect.top + stageRect.height() / 2f,
        stageRect.right,
        stageRect.top + stageRect.height() / 2f,
        gridPaint,
    )

    val stageLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Backdrop", stageRect.centerX(), stageRect.top - 20f, stageLabelPaint)
    canvas.drawText("Camera side", stageRect.centerX(), stageRect.bottom + 44f, stageLabelPaint)

    items.forEach { item ->
        drawLightingSetupMarker(
            canvas = canvas,
            stageRect = stageRect,
            item = item,
        )
    }

    return bitmap
}

private fun drawLightingSetupMarker(
    canvas: Canvas,
    stageRect: RectF,
    item: LightingSetupDraftItem,
) {
    val centerX = stageRect.left + stageRect.width() * item.xFraction
    val centerY = stageRect.top + stageRect.height() * item.yFraction
    val markerSize = min(stageRect.width(), stageRect.height()) * 0.06f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lightingSetupMarkerColor(item)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    val markerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }

    when (item.type) {
        LightingSetupItemType.CAMERA -> {
            val rect = RectF(
                centerX - markerSize * 1.25f,
                centerY - markerSize * 0.72f,
                centerX + markerSize * 1.25f,
                centerY + markerSize * 0.72f,
            )
            canvas.drawRoundRect(rect, 26f, 26f, paint)
        }

        LightingSetupItemType.SUBJECT -> {
            canvas.drawCircle(centerX, centerY, markerSize, paint)
        }

        LightingSetupItemType.LIGHT -> {
            canvas.drawCircle(centerX, centerY, markerSize, paint)
        }
    }

    val markerCode = when (item.type) {
        LightingSetupItemType.CAMERA -> "CAM"
        LightingSetupItemType.SUBJECT -> "SUB"
        LightingSetupItemType.LIGHT -> item.label.take(3).uppercase(Locale.US)
    }
    canvas.drawText(markerCode, centerX, centerY + 9f, markerTextPaint)
    canvas.drawText(item.label.ifBlank { item.type.defaultLabel }, centerX, centerY + markerSize + 38f, labelPaint)
}

private fun lightingSetupMarkerColor(item: LightingSetupDraftItem): Int = when (item.type) {
    LightingSetupItemType.CAMERA -> Color.parseColor("#334155")
    LightingSetupItemType.SUBJECT -> Color.parseColor("#0F766E")
    LightingSetupItemType.LIGHT -> when (item.label.trim().lowercase(Locale.US)) {
        "key" -> Color.parseColor("#F59E0B")
        "fill" -> Color.parseColor("#3B82F6")
        "back" -> Color.parseColor("#A855F7")
        else -> Color.parseColor("#E11D48")
    }
}

private fun slugifyLightingSetupName(name: String): String {
    val cleaned = name.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return if (cleaned.isBlank()) {
        "lighting-setup"
    } else {
        "lighting-setup-$cleaned"
    }
}

private fun formatLightingSetupShareTimestamp(updatedAtMillis: Long): String =
    java.time.Instant.ofEpochMilli(updatedAtMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
