package com.janhorak.shutterdeck.gear.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GearPresentationFormattingTest {

    @Test
    fun blankThreadSizeReturnsNull() {
        assertNull(normalizeThreadSize(""))
        assertNull(normalizeThreadSize("   "))
    }

    @Test
    fun standardMillimeterInputsNormalizeTogether() {
        assertEquals("67", normalizeThreadSize("67"))
        assertEquals("67", normalizeThreadSize("67mm"))
        assertEquals("67", normalizeThreadSize("67 mm"))
    }

    @Test
    fun systemStyleInputsStillUseNumericSizeKey() {
        assertEquals("100", normalizeThreadSize("100mm system"))
        assertEquals("82", normalizeThreadSize("82 mm holder"))
    }

    @Test
    fun nonNumericFallbackKeepsComparableTextKey() {
        assertEquals("reargel", normalizeThreadSize("rear gel"))
        assertEquals("drop-in", normalizeThreadSize("drop-in"))
    }

    @Test
    fun referencePhotoLabelUsesLastPathSegment() {
        assertEquals(
            "image:62",
            referencePhotoLabel("content://media/external/images/media/image%3A62"),
        )
    }

    @Test
    fun blankReferencePhotoLabelReturnsBlank() {
        assertEquals("", referencePhotoLabel(""))
        assertEquals("", referencePhotoLabel("   "))
    }
}
