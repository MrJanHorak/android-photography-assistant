package com.janhorak.shutterdeck.film.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FilmRollExportTest {
    @Test
    fun buildFilmRollCsv_includesMetadataAndEscapesFrameNotes() {
        val csv = buildFilmRollCsv(
            FilmRollExportBundle(
                metadata = FilmRollExportMetadata(
                    title = "Trip roll",
                    status = "Finished",
                    stockDisplayName = "Kodak Portra 400",
                    stockFormat = "35mm",
                    stockType = "Color negative",
                    stockProcessingType = "C-41",
                    stockBaseIso = 400,
                    stockReciprocityExponent = 1.11,
                    stockReciprocityStartsAtSeconds = 1.0,
                    exposureIndex = 800,
                    totalFrames = 36,
                    cameraLabel = "Nikon FM2",
                    lensLabel = "50mm f/1.4",
                    startedOnText = "2026-05-30",
                    finishedOnText = "2026-06-01",
                    notes = "Bracketed interiors",
                    generatedAt = LocalDateTime.of(2026, 5, 30, 10, 15),
                ),
                frames = listOf(
                    FilmRollExportFrame(
                        frameNumber = 12,
                        exposureSequence = 2,
                        apertureText = "f/5.6",
                        shutterSpeedText = "1/60",
                        focalLengthText = "50mm",
                        capturedAtText = "2026-05-30 18:42",
                        latitude = 49.1951,
                        longitude = 16.6068,
                        notes = "Double exposure, \"neon\" test",
                    ),
                ),
            ),
        )

        assertTrue(csv.contains("\"Roll title\",\"Trip roll\""))
        assertTrue(csv.contains("\"Processing\",\"C-41\""))
        assertTrue(csv.contains("\"Frame\",\"Exposure\",\"Aperture\",\"Shutter\",\"Focal length\",\"Captured at\",\"Latitude\",\"Longitude\",\"Notes\""))
        assertTrue(csv.contains("\"12\",\"2\",\"f/5.6\",\"1/60\",\"50mm\",\"2026-05-30 18:42\",\"49.1951\",\"16.6068\",\"Double exposure, \"\"neon\"\" test\""))
    }

    @Test
    fun defaultFilmRollCsvFileName_prefersTitleThenStockName() {
        assertEquals(
            "trip-roll-2026-05-30.csv",
            defaultFilmRollCsvFileName(
                title = "Trip roll",
                stockDisplayName = "Kodak Portra 400",
                date = LocalDate.of(2026, 5, 30),
            ),
        )
        assertEquals(
            "kodak-portra-400-2026-05-30.csv",
            defaultFilmRollCsvFileName(
                title = "",
                stockDisplayName = "Kodak Portra 400",
                date = LocalDate.of(2026, 5, 30),
            ),
        )
    }
}
