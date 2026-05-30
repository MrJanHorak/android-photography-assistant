package com.janhorak.shutterdeck.utilities.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrayCardReferenceTest {

    @Test
    fun gray18_usesGammaCorrectedSrgbValue() {
        val gray = GrayCardReference.GRAY_18

        assertEquals(0x76, gray.red)
        assertEquals(0x76, gray.green)
        assertEquals(0x76, gray.blue)
    }

    @Test
    fun whitePreset_usesDarkSystemBarIcons() {
        assertTrue(GrayCardReference.WHITE.useDarkSystemBarIcons)
        assertFalse(GrayCardReference.GRAY_18.useDarkSystemBarIcons)
        assertFalse(GrayCardReference.BLACK.useDarkSystemBarIcons)
    }

    @Test
    fun presets_keepExpectedLabels() {
        assertEquals(
            listOf("18% Gray", "White", "Black"),
            GrayCardReference.entries.map { it.label },
        )
    }
}
