package com.janhorak.shutterdeck.home

import com.janhorak.shutterdeck.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenToolCatalogTest {

    @Test
    fun favoriteTools_areAlphabetized_andIgnoreUnknownRoutes() {
        val favorites = favoriteTools(
            sections = filterToolSections(query = ""),
            favoriteRoutes = setOf(Routes.LIGHT_METER, Routes.DEW_POINT, "missing"),
        )

        assertEquals(listOf("Dew Point", "Light Meter"), favorites.map { it.title })
    }

    @Test
    fun filterToolSections_matchesIndividualToolText() {
        val sections = filterToolSections(query = "slate")

        assertEquals(listOf("On-Shoot Utilities"), sections.map { it.title })
        assertEquals(listOf("Digital Slate"), sections.single().tools.map { it.title })
    }

    @Test
    fun filterToolSections_keepsWholeSection_whenSectionHeadingMatches() {
        val sections = filterToolSections(query = "Planning & Output")

        assertEquals(1, sections.size)
        assertEquals("Planning & Output", sections.single().title)
        assertTrue(sections.single().tools.size > 1)
    }

    @Test
    fun recentTools_preserveRecentOrder_andIgnoreUnknownRoutes() {
        val recentEntries = recentTools(
            sections = filterToolSections(query = ""),
            recentRoutes = listOf(Routes.DIGITAL_SLATE, "missing", Routes.LIGHT_METER),
        )

        assertEquals(listOf("Digital Slate", "Light Meter"), recentEntries.map { it.title })
    }

    @Test
    fun recentTools_excludeFavoritedRoutes_fromRecentSectionOnly() {
        val recentEntries = recentTools(
            sections = filterToolSections(query = ""),
            recentRoutes = listOf(Routes.DIGITAL_SLATE, Routes.LIGHT_METER, Routes.DEW_POINT),
            excludedRoutes = setOf(Routes.LIGHT_METER),
        )

        assertEquals(listOf("Digital Slate", "Dew Point"), recentEntries.map { it.title })
    }
}
