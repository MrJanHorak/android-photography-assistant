package com.janhorak.shutterdeck.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryRecentRoutesTest {

    @Test
    fun updatedRecentRoutes_movesExistingRouteToFront_andCapsList() {
        val updatedRoutes = updatedRecentRoutes(
            currentRoutes = listOf("light-meter", "dew-point", "digital-slate", "shot-notes"),
            openedRoute = "dew-point",
            maxSize = 3,
        )

        assertEquals(listOf("dew-point", "light-meter", "digital-slate"), updatedRoutes)
    }

    @Test
    fun encodeAndDecodeRecentRoutes_roundTripWithoutBlankEntries() {
        val encodedRoutes = encodeRecentRoutes(listOf("light-meter", "digital-slate", "shot-notes"))
        val decodedRoutes = decodeRecentRoutes("$encodedRoutes\n\n")

        assertEquals(listOf("light-meter", "digital-slate", "shot-notes"), decodedRoutes)
    }
}
