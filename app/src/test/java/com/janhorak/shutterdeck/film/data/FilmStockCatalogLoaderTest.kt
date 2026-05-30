package com.janhorak.shutterdeck.film.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilmStockCatalogLoaderTest {
    @Test
    fun parse_readsValidCatalogEntries() {
        val parsedStocks = FilmStockCatalogLoader.parse(
            """
            {
              "stocks": [
                {
                  "id": "hp5_plus_400",
                  "brand": "Ilford",
                  "name": "HP5 Plus 400",
                  "format": "Multi-format",
                  "stockType": "B&W negative",
                  "iso": 400,
                  "reciprocityExponent": 1.31,
                  "reciprocityStartsAtSeconds": 1.0,
                  "processingType": "B&W",
                  "developerNotes": "ID-11 1+1",
                  "description": "Classic pushable stock",
                  "maxPushStops": 3,
                  "maxPullStops": 1
                },
                {
                  "id": "polaroid_color_i_type",
                  "brand": "Polaroid",
                  "name": "Color i-Type",
                  "format": "Instant",
                  "stockType": "Instant",
                  "iso": 640,
                  "reciprocityExponent": null,
                  "reciprocityStartsAtSeconds": null,
                  "processingType": "Instant",
                  "developerNotes": "Keep warm in winter",
                  "description": "Integral instant film",
                  "maxPushStops": null,
                  "maxPullStops": null
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(2, parsedStocks.size)
        assertEquals("Ilford", parsedStocks.first().brand)
        assertEquals(1.31, parsedStocks.first().reciprocityExponent!!, 1e-9)
        assertEquals("Instant", parsedStocks.last().processingType)
        assertNull(parsedStocks.last().reciprocityExponent)
        assertNull(parsedStocks.last().maxPushStops)
    }

    @Test
    fun parse_rejectsDuplicateIds() {
        val error = runCatching {
            FilmStockCatalogLoader.parse(
                """
                {
                  "stocks": [
                    {
                      "id": "duplicate",
                      "brand": "Brand A",
                      "name": "Stock A",
                      "format": "35mm",
                      "stockType": "Color negative",
                      "iso": 100,
                      "reciprocityExponent": 1.1,
                      "reciprocityStartsAtSeconds": 1.0,
                      "processingType": "C-41"
                    },
                    {
                      "id": "duplicate",
                      "brand": "Brand B",
                      "name": "Stock B",
                      "format": "120",
                      "stockType": "Slide",
                      "iso": 100,
                      "reciprocityExponent": 1.2,
                      "reciprocityStartsAtSeconds": 1.0,
                      "processingType": "E-6"
                    }
                  ]
                }
                """.trimIndent(),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message?.contains("Duplicate IDs") == true)
    }

    @Test
    fun parse_requiresBothReciprocityFields() {
        val error = runCatching {
            FilmStockCatalogLoader.parse(
                """
                {
                  "stocks": [
                    {
                      "id": "broken_stock",
                      "brand": "Brand A",
                      "name": "Broken Stock",
                      "format": "35mm",
                      "stockType": "B&W negative",
                      "iso": 400,
                      "reciprocityExponent": 1.3,
                      "processingType": "B&W"
                    }
                  ]
                }
                """.trimIndent(),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message?.contains("reciprocityStartsAtSeconds") == true)
    }
}
