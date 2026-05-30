package com.janhorak.shutterdeck.film.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class FilmRollExportMetadata(
    val title: String,
    val status: String,
    val stockDisplayName: String,
    val stockFormat: String,
    val stockType: String,
    val stockProcessingType: String,
    val stockBaseIso: Int?,
    val stockReciprocityExponent: Double?,
    val stockReciprocityStartsAtSeconds: Double?,
    val exposureIndex: Int,
    val totalFrames: Int?,
    val cameraLabel: String,
    val lensLabel: String,
    val startedOnText: String,
    val finishedOnText: String,
    val notes: String,
    val generatedAt: LocalDateTime,
)

data class FilmRollExportFrame(
    val frameNumber: Int,
    val exposureSequence: Int,
    val apertureText: String,
    val shutterSpeedText: String,
    val focalLengthText: String,
    val capturedAtText: String,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String,
)

data class FilmRollExportBundle(
    val metadata: FilmRollExportMetadata,
    val frames: List<FilmRollExportFrame>,
)

private val filmRollExportDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun buildFilmRollDisplayTitle(
    title: String,
    stockDisplayName: String,
): String {
    return title.trim().ifBlank { stockDisplayName.trim().ifBlank { "Untitled roll" } }
}

fun buildFilmRollCsv(bundle: FilmRollExportBundle): String {
    val metadata = bundle.metadata
    return buildString {
        appendCsvRow("Roll title", buildFilmRollDisplayTitle(metadata.title, metadata.stockDisplayName))
        appendCsvRow("Status", metadata.status)
        appendCsvRow("Stock", metadata.stockDisplayName)
        appendCsvRow("Stock format", metadata.stockFormat)
        appendCsvRow("Stock type", metadata.stockType)
        appendCsvRow("Processing", metadata.stockProcessingType)
        appendCsvRow("Base ISO", metadata.stockBaseIso?.toString())
        appendCsvRow("Rated ISO", metadata.exposureIndex.toString())
        appendCsvRow("Reciprocity exponent", formatStableDecimal(metadata.stockReciprocityExponent))
        appendCsvRow("Reciprocity starts at (s)", formatStableDecimal(metadata.stockReciprocityStartsAtSeconds))
        appendCsvRow("Roll capacity", metadata.totalFrames?.toString())
        appendCsvRow("Camera", metadata.cameraLabel)
        appendCsvRow("Lens", metadata.lensLabel)
        appendCsvRow("Started on", metadata.startedOnText)
        appendCsvRow("Finished on", metadata.finishedOnText)
        appendCsvRow("Notes", metadata.notes)
        appendCsvRow("Generated at", metadata.generatedAt.format(filmRollExportDateTimeFormatter))
        appendLine()
        appendCsvRow(
            "Frame",
            "Exposure",
            "Aperture",
            "Shutter",
            "Focal length",
            "Captured at",
            "Latitude",
            "Longitude",
            "Notes",
        )
        bundle.frames.forEach { frame ->
            appendCsvRow(
                frame.frameNumber.toString(),
                frame.exposureSequence.toString(),
                frame.apertureText,
                frame.shutterSpeedText,
                frame.focalLengthText,
                frame.capturedAtText,
                formatStableDecimal(frame.latitude),
                formatStableDecimal(frame.longitude),
                frame.notes,
            )
        }
    }
}

fun defaultFilmRollCsvFileName(
    title: String,
    stockDisplayName: String,
    date: LocalDate = LocalDate.now(),
): String {
    val normalizedBase = buildFilmRollDisplayTitle(title, stockDisplayName)
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "film-roll" }
    return "$normalizedBase-$date.csv"
}

private fun StringBuilder.appendCsvRow(vararg columns: String?) {
    appendLine(columns.joinToString(",") { value -> value.toCsvColumn() })
}

private fun String?.toCsvColumn(): String {
    val normalized = this.orEmpty()
    return "\"${normalized.replace("\"", "\"\"")}\""
}

private fun formatStableDecimal(value: Double?): String {
    if (value == null) return ""
    val formatted = String.format(Locale.ROOT, "%.6f", value)
    return formatted.trimEnd('0').trimEnd('.')
}
