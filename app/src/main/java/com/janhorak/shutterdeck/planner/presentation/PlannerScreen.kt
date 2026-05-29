package com.janhorak.shutterdeck.planner.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.navigation.Routes
import com.janhorak.shutterdeck.ui.components.SectionHeader
import com.janhorak.shutterdeck.ui.components.ToolCard

private data class PlannerEntry(val title: String, val subtitle: String, val route: String)

private val plannerTools = listOf(
    PlannerEntry("Golden Hour", "Sunrise, sunset, golden & blue hour", Routes.SUN_TIMES),
    PlannerEntry("Sun & Moon", "Position, azimuth & moon phase", Routes.SUN_MOON_POSITION),
    PlannerEntry("Scouting Locations", "Saved spots with notes & best time", Routes.LOCATIONS),
    PlannerEntry("Shoots", "Shot lists & shoot checklists", Routes.SHOOTS),
)

/** Planning hub: links to time/astronomy tools and the saved-data planners. */
@Composable
fun PlannerScreen(
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Plan your shoot",
                subtitle = "Timing, light direction, locations and shot lists in one place.",
            )
        }
        items(plannerTools) { tool ->
            ToolCard(
                title = tool.title,
                subtitle = tool.subtitle,
                enabled = true,
                onClick = { onOpen(tool.route) },
            )
        }
    }
}
