package com.janhorak.shutterdeck.film.presentation

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

private data class FilmEntry(val title: String, val subtitle: String, val route: String?)

private val filmTools = listOf(
    FilmEntry(
        title = "Film Stocks",
        subtitle = "Bundled starter stocks plus your own reciprocity and development notes.",
        route = Routes.FILM_STOCKS,
    ),
    FilmEntry(
        title = "Roll Logger",
        subtitle = "Track rolls, frame counts and development state from the same stock library.",
        route = null,
    ),
    FilmEntry(
        title = "Development Timer",
        subtitle = "Reusable chemistry timers tied to your processing workflow.",
        route = null,
    ),
    FilmEntry(
        title = "Push / Pull Helper",
        subtitle = "Quick compensation reference based on each stock's latitude.",
        route = null,
    ),
    FilmEntry(
        title = "Reciprocity Assistant",
        subtitle = "Use stock-aware reciprocity data inside the long-exposure workflow.",
        route = null,
    ),
)

@Composable
fun FilmScreen(
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
                title = "Film workflow",
                subtitle = "Stocks, rolls, development and reciprocity tools in one place.",
            )
        }
        items(filmTools) { tool ->
            ToolCard(
                title = tool.title,
                subtitle = tool.subtitle,
                enabled = tool.route != null,
                onClick = { tool.route?.let(onOpen) },
            )
        }
    }
}
