package com.janhorak.shutterdeck.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.navigation.Routes
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader
import com.janhorak.shutterdeck.ui.components.ToolCard

private data class ToolEntry(
    val title: String,
    val subtitle: String,
    val route: String?,
) {
    val enabled: Boolean get() = route != null
}

private data class ToolSection(
    val title: String,
    val subtitle: String,
    val tools: List<ToolEntry>,
)

private val toolSections = listOf(
    ToolSection(
        title = "Exposure",
        subtitle = "Metering, flash, daylight rules and shutter-speed tradeoffs.",
        tools = listOf(
            ToolEntry("Light Meter", "Ambient & reflective metering", Routes.LIGHT_METER),
            ToolEntry("EV / Lux", "Ambient EV100, lux & foot-candles", Routes.EV_LUX),
            ToolEntry("Color Temperature", "Kelvin ↔ mired white-balance reference", Routes.COLOR_TEMPERATURE),
            ToolEntry("Unit Converter", "ft ↔ m and °C ↔ °F quick reference", Routes.UNIT_CONVERTER),
            ToolEntry("ND Filter", "Long-exposure calculator", Routes.ND_FILTER),
            ToolEntry("Sunny 16", "Daylight exposure & reciprocity", Routes.SUNNY_16),
            ToolEntry("Equivalent Exposure", "Trade aperture, shutter & ISO", Routes.EQUIVALENT_EXPOSURE),
            ToolEntry("Guide Number", "Flash distance & aperture", Routes.GUIDE_NUMBER),
            ToolEntry("Astro Shutter", "500 / NPF rule for stars", Routes.ASTRO_SHUTTER),
        ),
    ),
    ToolSection(
        title = "Lens & Focus",
        subtitle = "Depth, framing, macro and sharpness planning.",
        tools = listOf(
            ToolEntry("Depth of Field", "Hyperfocal & near/far limits", Routes.DEPTH_OF_FIELD),
            ToolEntry("Focus Stacking", "Frames to cover a depth range", Routes.FOCUS_STACK),
            ToolEntry("Field of View", "Angle of view & 35mm equivalent", Routes.FIELD_OF_VIEW),
            ToolEntry("Macro / Extension", "Magnification & exposure comp", Routes.MACRO),
            ToolEntry("Diffraction Limit", "Sharpest aperture for your sensor", Routes.DIFFRACTION),
        ),
    ),
    ToolSection(
        title = "Planning & Output",
        subtitle = "Sun position, timing and print sizing references.",
        tools = listOf(
            ToolEntry("Golden Hour", "Sunrise, sunset, golden & blue hour", Routes.SUN_TIMES),
            ToolEntry("Sun & Moon", "Position, azimuth & moon phase", Routes.SUN_MOON_POSITION),
            ToolEntry("Print Size", "Pixels to print size at DPI", Routes.PRINT_SIZE),
        ),
    ),
    ToolSection(
        title = "On-Shoot Utilities",
        subtitle = "Quick field references, live screens, and sensor-aided utilities.",
        tools = listOf(
            ToolEntry("Intervalometer", "Time-lapse duration, clip, card & battery planning", Routes.INTERVALOMETER),
            ToolEntry("Dew Point", "Lens-fog warning from temperature and humidity", Routes.DEW_POINT),
            ToolEntry("Digital Slate", "Scene / shot / take board with a sync flash mark", Routes.DIGITAL_SLATE),
            ToolEntry("Shot Notes", "Voice/text notes with timestamps and current location", Routes.SHOT_NOTES),
            ToolEntry("Lighting Setup", "Drag camera, subject and lights into a reusable diagram", Routes.LIGHTING_SETUP),
            ToolEntry("Spirit Level", "Pitch / roll bubble level", Routes.SPIRIT_LEVEL),
            ToolEntry("Gray Card", "Full-screen gray / white / black reference", Routes.GRAY_CARD),
            ToolEntry("Composition Overlays", "Rule-of-thirds, golden ratio & diagonals", Routes.COMPOSITION_OVERLAYS),
            ToolEntry("Live Histogram & Zebra", "Live luminance distribution & clipped highlights", Routes.LIVE_HISTOGRAM),
        ),
    ),
)

/** Home hub: a grid of available tools. */
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = searchQuery.trim()
    val filteredSections = if (normalizedQuery.isBlank()) {
        toolSections
    } else {
        toolSections.mapNotNull { section ->
            val sectionMatches = section.title.contains(normalizedQuery, ignoreCase = true) ||
                section.subtitle.contains(normalizedQuery, ignoreCase = true)
            val matchingTools = if (sectionMatches) {
                section.tools
            } else {
                section.tools.filter { tool ->
                    tool.title.contains(normalizedQuery, ignoreCase = true) ||
                        tool.subtitle.contains(normalizedQuery, ignoreCase = true)
                }
            }
            matchingTools.takeIf { it.isNotEmpty() }?.let { section.copy(tools = it) }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "Tools",
                subtitle = "Exposure, lens and planning tools grouped for quicker scanning.",
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LabeledField(
                label = "Search tools",
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier,
                singleLine = true,
            )
        }
        if (filteredSections.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "No matching tools",
                    subtitle = "Try a tool name like meter, slate, map, film, or print.",
                )
            }
        }
        filteredSections.forEach { section ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = section.title,
                    subtitle = section.subtitle,
                )
            }
            items(section.tools, key = { "${section.title}:${it.title}" }) { tool ->
                ToolCard(
                    title = tool.title,
                    subtitle = tool.subtitle,
                    enabled = tool.enabled,
                    onClick = { tool.route?.let(onToolClick) },
                )
            }
        }
    }
}
