package com.janhorak.shutterdeck.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.AppViewModel
import com.janhorak.shutterdeck.core.ThemeMode
import com.janhorak.shutterdeck.ui.components.SectionHeader

private data class HelpItem(
    val title: String,
    val description: String,
)

private val tabOverviewItems = listOf(
    HelpItem(
        title = "Tools",
        description = "Exposure references, planning math, and on-shoot utilities such as the light meter, digital slate, shot notes, and setup diagrammer. Search here, star your regular tools to pin them at the top, and use Recent to jump back into the tools you opened last.",
    ),
    HelpItem(
        title = "Planner",
        description = "Scouting locations, saved shoots, shot checklists, and astronomy helpers for planning a session.",
    ),
    HelpItem(
        title = "Gear",
        description = "Inventory, filters, batteries, memory cards, kits, loans, maintenance, and insurance/export summaries.",
    ),
    HelpItem(
        title = "Film",
        description = "Film stocks, roll logging, development timing, push-pull notes, and reciprocity guidance.",
    ),
    HelpItem(
        title = "More",
        description = "Theme settings plus the quick help notes on this screen.",
    ),
)

private val fieldTipItems = listOf(
    HelpItem(
        title = "Immersive tools",
        description = "Gray Card, Spirit Level, Digital Slate, Composition Overlays, and Live Histogram open in a focused full-screen mode. Use the system Back gesture or back button to leave them.",
    ),
    HelpItem(
        title = "Permissions",
        description = "Camera is only requested for metering and live-preview tools, location for planning/astronomy and note helpers, and speech recognition for shot-note dictation.",
    ),
    HelpItem(
        title = "Local-first data",
        description = "Most saved data stays on this device unless you explicitly export, share, or attach a document/photo through the system picker.",
    ),
    HelpItem(
        title = "Good first stops",
        description = "Try Tools for fast references, Planner for locations and shoots, Gear for inventory, and Film for analog workflows.",
    ),
)

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "Follow system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
    ThemeMode.NIGHT -> "Night vision (red)"
}

private fun ThemeMode.description(): String = when (this) {
    ThemeMode.SYSTEM -> "Match the device light/dark setting."
    ThemeMode.LIGHT -> "Bright theme for daylight use."
    ThemeMode.DARK -> "OLED-friendly dark theme."
    ThemeMode.NIGHT -> "Red-on-black to preserve night vision for astro."
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            title = "Settings & help",
            subtitle = "Choose how ShutterDeck looks, then use the notes below as a quick orientation guide.",
        )
        SectionHeader(title = "Appearance", subtitle = "Choose how ShutterDeck looks.")
        ThemeMode.entries.forEach { mode ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                    .padding(vertical = 8.dp),
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                    Text(
                        text = mode.displayName(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    text = mode.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp),
                )
            }
        }
        HelpCard(
            title = "Getting started",
            subtitle = "If you are opening ShutterDeck for the first time, this is the quickest map of the app.",
            items = tabOverviewItems,
        )
        HelpCard(
            title = "Field tips",
            subtitle = "A few behavior and permission notes that explain the less obvious tools.",
            items = fieldTipItems,
        )
    }
}

@Composable
private fun HelpCard(
    title: String,
    subtitle: String,
    items: List<HelpItem>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(title = title, subtitle = subtitle)
            items.forEachIndexed { index, item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index != items.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
