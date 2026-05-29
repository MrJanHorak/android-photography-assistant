package com.janhorak.shutterdeck.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.navigation.Routes
import com.janhorak.shutterdeck.ui.components.ToolCard

private data class ToolEntry(
    val title: String,
    val subtitle: String,
    val route: String?,
) {
    val enabled: Boolean get() = route != null
}

private val tools = listOf(
    ToolEntry("Light Meter", "Ambient & reflective metering", Routes.LIGHT_METER),
    ToolEntry("Golden Hour", "Sunrise, sunset, golden & blue hour", Routes.SUN_TIMES),
    ToolEntry("Depth of Field", "Hyperfocal & near/far limits", Routes.DEPTH_OF_FIELD),
    ToolEntry("Focus Stacking", "Frames to cover a depth range", Routes.FOCUS_STACK),
    ToolEntry("ND Filter", "Long-exposure calculator", Routes.ND_FILTER),
    ToolEntry("Field of View", "Angle of view & 35mm equivalent", Routes.FIELD_OF_VIEW),
    ToolEntry("Astro Shutter", "500 / NPF rule for stars", Routes.ASTRO_SHUTTER),
    ToolEntry("Sunny 16", "Daylight exposure & reciprocity", Routes.SUNNY_16),
    ToolEntry("Equivalent Exposure", "Trade aperture, shutter & ISO", Routes.EQUIVALENT_EXPOSURE),
    ToolEntry("Guide Number", "Flash distance & aperture", Routes.GUIDE_NUMBER),
    ToolEntry("Macro / Extension", "Magnification & exposure comp", Routes.MACRO),
    ToolEntry("Diffraction Limit", "Sharpest aperture for your sensor", Routes.DIFFRACTION),
    ToolEntry("Print Size", "Pixels to print size at DPI", Routes.PRINT_SIZE),
)

/** Home hub: a grid of available tools. */
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(tools) { tool ->
            ToolCard(
                title = tool.title,
                subtitle = tool.subtitle,
                enabled = tool.enabled,
                onClick = { tool.route?.let(onToolClick) },
            )
        }
    }
}
