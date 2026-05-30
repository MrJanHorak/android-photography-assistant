package com.janhorak.shutterdeck.film.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class FilmStockSourceFilter(val label: String) {
    ALL("All"),
    BUNDLED("Bundled"),
    CUSTOM("Custom"),
}

private data class FilmStockLibrarySummary(
    val totalCount: Int,
    val bundledCount: Int,
    val customCount: Int,
    val reciprocityCount: Int,
    val processingCount: Int,
)

private const val ALL_FORMATS = "All formats"
private const val ALL_TYPES = "All types"

private val filmFormatOptions = listOf(
    "35mm",
    "120",
    "Sheet",
    "35mm / 120",
    "Multi-format",
    "Instant",
    "Other",
)

private val filmStockTypeOptions = listOf(
    "Color negative",
    "B&W negative",
    "Slide",
    "Instant",
    "Other",
)

private val filmProcessingTypeOptions = listOf(
    "C-41",
    "E-6",
    "B&W",
    "ECN-2",
    "Instant",
    "Other",
)

@Composable
fun FilmStocksScreen(
    modifier: Modifier = Modifier,
    viewModel: FilmStocksViewModel = hiltViewModel(),
) {
    val stocks by viewModel.stocks.collectAsStateWithLifecycle()
    var showingNewEditor by remember { mutableStateOf(false) }
    var editingStock by remember { mutableStateOf<FilmStockEntity?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sourceFilterName by rememberSaveable { mutableStateOf(FilmStockSourceFilter.ALL.name) }
    var formatFilter by rememberSaveable { mutableStateOf(ALL_FORMATS) }
    var typeFilter by rememberSaveable { mutableStateOf(ALL_TYPES) }

    val sourceFilter = remember(sourceFilterName) {
        FilmStockSourceFilter.valueOf(sourceFilterName)
    }
    val summary = remember(stocks) { buildFilmStockSummary(stocks) }
    val filteredStocks = remember(stocks, searchQuery, sourceFilter, formatFilter, typeFilter) {
        stocks.filter { stock ->
            stock.matchesQuery(searchQuery) &&
                stock.matchesSource(sourceFilter) &&
                (formatFilter == ALL_FORMATS || stock.format == formatFilter) &&
                (typeFilter == ALL_TYPES || stock.stockType == typeFilter)
        }
    }
    val customStocks = remember(filteredStocks) {
        filteredStocks.filter { stock -> !stock.isBuiltIn }
    }
    val bundledStocks = remember(filteredStocks) {
        filteredStocks.filter { stock -> stock.isBuiltIn }
    }
    val showEditor = showingNewEditor || editingStock != null

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Film stocks",
                subtitle = "Bundled starter stocks plus your own custom reciprocity, process and push/pull notes. Bundled entries are read-only.",
            )
        }
        item {
            FilmStockSummaryCard(summary = summary)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionHeader(
                        title = "Find and extend your stock library",
                        subtitle = "Search by brand, stock name or notes, then add your own custom entries when the bundled starter list is not enough.",
                    )
                    LabeledField(
                        label = "Search",
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        keyboardType = KeyboardType.Text,
                    )
                    SelectableChipRow(
                        label = "Source",
                        options = FilmStockSourceFilter.entries.map { option -> option.label },
                        selectedOption = sourceFilter.label,
                        onSelected = { selected ->
                            sourceFilterName = FilmStockSourceFilter.entries
                                .first { option -> option.label == selected }
                                .name
                        },
                    )
                    SelectableChipRow(
                        label = "Format",
                        options = listOf(ALL_FORMATS) + filmFormatOptions,
                        selectedOption = formatFilter,
                        onSelected = { formatFilter = it },
                    )
                    SelectableChipRow(
                        label = "Type",
                        options = listOf(ALL_TYPES) + filmStockTypeOptions,
                        selectedOption = typeFilter,
                        onSelected = { typeFilter = it },
                    )
                    OutlinedButton(
                        onClick = {
                            showingNewEditor = !showingNewEditor
                            editingStock = null
                        },
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Text(
                            text = if (showingNewEditor) "Hide custom stock editor" else "Add custom stock",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
        if (showEditor) {
            item {
                FilmStockEditorCard(
                    initial = editingStock,
                    onCancel = {
                        showingNewEditor = false
                        editingStock = null
                    },
                    onSave = { initial, brand, name, format, stockType, iso, reciprocityExponent, reciprocityStartsAtSeconds, processingType, maxPushStops, maxPullStops, developerNotes, description ->
                        viewModel.save(
                            existing = initial,
                            brand = brand,
                            name = name,
                            format = format,
                            stockType = stockType,
                            iso = iso,
                            reciprocityExponent = reciprocityExponent,
                            reciprocityStartsAtSeconds = reciprocityStartsAtSeconds,
                            processingType = processingType,
                            maxPushStops = maxPushStops,
                            maxPullStops = maxPullStops,
                            developerNotes = developerNotes,
                            description = description,
                        )
                        showingNewEditor = false
                        editingStock = null
                    },
                )
            }
        }
        if (filteredStocks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "No film stocks match the current filters.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Clear the search or filters, or add a custom stock that fits the way you shoot.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (customStocks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Custom stocks",
                    subtitle = "${customStocks.size} editable entries",
                )
            }
            items(customStocks, key = { stock -> stock.id }) { stock ->
                FilmStockCard(
                    stock = stock,
                    onEdit = {
                        editingStock = stock
                        showingNewEditor = false
                    },
                    onDelete = {
                        if (editingStock?.id == stock.id) {
                            editingStock = null
                            showingNewEditor = false
                        }
                        viewModel.delete(stock)
                    },
                )
            }
        }
        if (bundledStocks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Bundled stocks",
                    subtitle = "${bundledStocks.size} starter entries",
                )
            }
            items(bundledStocks, key = { stock -> stock.id }) { stock ->
                FilmStockCard(
                    stock = stock,
                    onEdit = null,
                    onDelete = null,
                )
            }
        }
    }
}

@Composable
private fun FilmStockSummaryCard(summary: FilmStockLibrarySummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(
                title = "Library summary",
                subtitle = "Approximate reciprocity fits are only a starting point; save your own custom stock when your notes differ.",
            )
            SummaryLine(label = "Stocks", value = summary.totalCount.toString())
            SummaryLine(label = "Bundled", value = summary.bundledCount.toString())
            SummaryLine(label = "Custom", value = summary.customCount.toString())
            SummaryLine(label = "With reciprocity curve", value = summary.reciprocityCount.toString())
            SummaryLine(label = "Processing types", value = summary.processingCount.toString())
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FilmStockEditorCard(
    initial: FilmStockEntity?,
    onSave: (
        FilmStockEntity?,
        String,
        String,
        String,
        String,
        Int?,
        Double?,
        Double?,
        String,
        Int?,
        Int?,
        String,
        String,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    var brand by remember(initial?.id) { mutableStateOf(initial?.brand.orEmpty()) }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var format by remember(initial?.id) { mutableStateOf(initial?.format ?: filmFormatOptions.first()) }
    var stockType by remember(initial?.id) { mutableStateOf(initial?.stockType ?: filmStockTypeOptions.first()) }
    var isoText by remember(initial?.id) { mutableStateOf(initial?.iso?.toString().orEmpty()) }
    var reciprocityExponentText by remember(initial?.id) {
        mutableStateOf(initial?.reciprocityExponent?.let(::formatCompactDouble).orEmpty())
    }
    var reciprocityStartsAtSecondsText by remember(initial?.id) {
        mutableStateOf(initial?.reciprocityStartsAtSeconds?.let(::formatCompactDouble).orEmpty())
    }
    var processingType by remember(initial?.id) {
        mutableStateOf(initial?.processingType ?: filmProcessingTypeOptions.first())
    }
    var maxPushStopsText by remember(initial?.id) {
        mutableStateOf(initial?.maxPushStops?.toString().orEmpty())
    }
    var maxPullStopsText by remember(initial?.id) {
        mutableStateOf(initial?.maxPullStops?.toString().orEmpty())
    }
    var developerNotes by remember(initial?.id) { mutableStateOf(initial?.developerNotes.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }

    val parsedIso = isoText.toIntOrNull()
    val parsedReciprocityExponent = reciprocityExponentText.takeIf { text -> text.isNotBlank() }?.toDoubleOrNull()
    val parsedReciprocityStartsAtSeconds = reciprocityStartsAtSecondsText.takeIf { text -> text.isNotBlank() }?.toDoubleOrNull()
    val parsedMaxPushStops = maxPushStopsText.takeIf { text -> text.isNotBlank() }?.toIntOrNull()
    val parsedMaxPullStops = maxPullStopsText.takeIf { text -> text.isNotBlank() }?.toIntOrNull()
    val hasReciprocityInputs = reciprocityExponentText.isNotBlank() || reciprocityStartsAtSecondsText.isNotBlank()
    val reciprocityInputsValid = !hasReciprocityInputs || (
        parsedReciprocityExponent != null &&
            parsedReciprocityExponent > 0.0 &&
            parsedReciprocityStartsAtSeconds != null &&
            parsedReciprocityStartsAtSeconds > 0.0
        )
    val pushInputValid = maxPushStopsText.isBlank() || (parsedMaxPushStops != null && parsedMaxPushStops >= 0)
    val pullInputValid = maxPullStopsText.isBlank() || (parsedMaxPullStops != null && parsedMaxPullStops >= 0)
    val saveEnabled = brand.isNotBlank() &&
        name.isNotBlank() &&
        parsedIso != null &&
        parsedIso > 0 &&
        reciprocityInputsValid &&
        pushInputValid &&
        pullInputValid

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = if (initial == null) "Custom stock editor" else "Edit custom stock",
                subtitle = "Fill both reciprocity fields or leave both blank. Built-in stocks stay read-only so your starter catalog always remains intact.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Brand",
                    value = brand,
                    onValueChange = { brand = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Stock name",
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            SelectableChipRow(
                label = "Format",
                options = filmFormatOptions,
                selectedOption = format,
                onSelected = { format = it },
            )
            SelectableChipRow(
                label = "Type",
                options = filmStockTypeOptions,
                selectedOption = stockType,
                onSelected = { stockType = it },
            )
            SelectableChipRow(
                label = "Processing",
                options = filmProcessingTypeOptions,
                selectedOption = processingType,
                onSelected = { processingType = it },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "ISO",
                    value = isoText,
                    onValueChange = { isoText = it },
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = "Reciprocity exponent",
                    value = reciprocityExponentText,
                    onValueChange = { reciprocityExponentText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Reciprocity starts at",
                    value = reciprocityStartsAtSecondsText,
                    onValueChange = { reciprocityStartsAtSecondsText = it },
                    modifier = Modifier.weight(1f),
                    suffix = "s",
                    keyboardType = KeyboardType.Decimal,
                )
                LabeledField(
                    label = "Max push",
                    value = maxPushStopsText,
                    onValueChange = { maxPushStopsText = it },
                    modifier = Modifier.weight(1f),
                    suffix = "stops",
                )
            }
            LabeledField(
                label = "Max pull",
                value = maxPullStopsText,
                onValueChange = { maxPullStopsText = it },
                suffix = "stops",
            )
            LabeledField(
                label = "Developer notes",
                value = developerNotes,
                onValueChange = { developerNotes = it },
                singleLine = false,
                keyboardType = KeyboardType.Text,
            )
            LabeledField(
                label = "Description",
                value = description,
                onValueChange = { description = it },
                singleLine = false,
                keyboardType = KeyboardType.Text,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            initial,
                            brand,
                            name,
                            format,
                            stockType,
                            parsedIso,
                            parsedReciprocityExponent,
                            parsedReciprocityStartsAtSeconds,
                            processingType,
                            parsedMaxPushStops,
                            parsedMaxPullStops,
                            developerNotes,
                            description,
                        )
                    },
                    enabled = saveEnabled,
                ) {
                    Text(if (initial == null) "Save custom stock" else "Update custom stock")
                }
            }
        }
    }
}

@Composable
private fun FilmStockCard(
    stock: FilmStockEntity,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stock.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (stock.isBuiltIn) "Bundled starter stock" else "Custom stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "ISO ${stock.iso} · ${stock.stockType} · ${stock.format} · ${stock.processingType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onEdit != null && onDelete != null) {
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit stock")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete stock")
                        }
                    }
                }
            }
            Text(
                text = formatReciprocitySummary(stock),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            formatPushPullSummary(stock)?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (stock.description.isNotBlank()) {
                Text(
                    text = stock.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (stock.developerNotes.isNotBlank()) {
                Text(
                    text = "Dev notes: ${stock.developerNotes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SelectableChipRow(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selectedOption,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                    modifier = Modifier.widthIn(min = 80.dp),
                )
            }
        }
    }
}

private fun buildFilmStockSummary(stocks: List<FilmStockEntity>): FilmStockLibrarySummary {
    return FilmStockLibrarySummary(
        totalCount = stocks.size,
        bundledCount = stocks.count { stock -> stock.isBuiltIn },
        customCount = stocks.count { stock -> !stock.isBuiltIn },
        reciprocityCount = stocks.count { stock ->
            stock.reciprocityExponent != null && stock.reciprocityStartsAtSeconds != null
        },
        processingCount = stocks.map { stock -> stock.processingType.lowercase() }.distinct().size,
    )
}

private fun FilmStockEntity.matchesQuery(query: String): Boolean {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return true
    return displayName.contains(trimmedQuery, ignoreCase = true) ||
        format.contains(trimmedQuery, ignoreCase = true) ||
        stockType.contains(trimmedQuery, ignoreCase = true) ||
        processingType.contains(trimmedQuery, ignoreCase = true) ||
        description.contains(trimmedQuery, ignoreCase = true) ||
        developerNotes.contains(trimmedQuery, ignoreCase = true)
}

private fun FilmStockEntity.matchesSource(sourceFilter: FilmStockSourceFilter): Boolean {
    return when (sourceFilter) {
        FilmStockSourceFilter.ALL -> true
        FilmStockSourceFilter.BUNDLED -> isBuiltIn
        FilmStockSourceFilter.CUSTOM -> !isBuiltIn
    }
}

private fun formatReciprocitySummary(stock: FilmStockEntity): String {
    val exponent = stock.reciprocityExponent
    val startsAt = stock.reciprocityStartsAtSeconds
    return if (exponent != null && startsAt != null) {
        "Reciprocity: approx. t^${formatCompactDouble(exponent)} after ${formatExposureTime(startsAt)}"
    } else {
        "Reciprocity: no curve saved yet"
    }
}

private fun formatPushPullSummary(stock: FilmStockEntity): String? {
    val parts = buildList {
        stock.maxPushStops?.takeIf { value -> value > 0 }?.let { value -> add("+$value push") }
        stock.maxPullStops?.takeIf { value -> value > 0 }?.let { value -> add("-$value pull") }
    }
    return parts.takeIf(List<String>::isNotEmpty)?.joinToString(prefix = "Latitude: ", separator = " · ")
}

private fun formatCompactDouble(value: Double): String {
    val roundedWhole = value.roundToInt().toDouble()
    return if (abs(value - roundedWhole) < 0.005) {
        roundedWhole.toInt().toString()
    } else {
        String.format("%.2f", value).trimEnd('0').trimEnd('.')
    }
}
