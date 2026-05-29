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
    ToolEntry("Depth of Field", "Hyperfocal & near/far limits", null),
    ToolEntry("ND Filter", "Long-exposure calculator", null),
    ToolEntry("Field of View", "Angle of view & 35mm equivalent", null),
    ToolEntry("Astro Shutter", "500 / NPF rule for stars", null),
    ToolEntry("Print Size", "Pixels to print size at DPI", null),
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
