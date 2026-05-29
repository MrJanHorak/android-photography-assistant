package com.janhorak.shutterdeck.gear.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.GearItemEntity
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader
import java.util.Locale

private val gearCategories = listOf("Body", "Lens", "Accessory")

@Composable
fun GearInventoryScreen(
    modifier: Modifier = Modifier,
    viewModel: GearInventoryViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<GearItemEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val bodyCount = items.count { it.category == "Body" }
    val lensCount = items.count { it.category == "Lens" }
    val accessoryCount = items.count { it.category == "Accessory" }
    val totalWeight = items.sumOf { it.weightGrams ?: 0.0 }
    val totalValue = items.sumOf { it.currentValue ?: 0.0 }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Gear inventory",
                subtitle = "Track bodies, lenses and accessories. Packing kits and maintenance logs come next.",
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    editing = null
                    showEditor = !showEditor
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add gear item")
            }
        }
        if (showEditor) {
            item {
                GearEditorCard(
                    initial = editing,
                    onSave = { id, category, brand, model, serial, purchaseDate, purchasePrice, currentValue, weightGrams, notes ->
                        viewModel.save(
                            id = id,
                            category = category,
                            brand = brand,
                            model = model,
                            serialNumber = serial,
                            purchaseDateText = purchaseDate,
                            purchasePrice = purchasePrice,
                            currentValue = currentValue,
                            weightGrams = weightGrams,
                            notes = notes,
                        )
                        editing = null
                        showEditor = false
                    },
                    onCancel = {
                        editing = null
                        showEditor = false
                    },
                )
            }
        }
        if (items.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ResultRow("Items", items.size.toString())
                        ResultRow("Bodies", bodyCount.toString())
                        ResultRow("Lenses", lensCount.toString())
                        ResultRow("Accessories", accessoryCount.toString())
                        ResultRow("Est. value", formatMoney(totalValue))
                        ResultRow("Total weight", formatWeight(totalWeight))
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item {
                Text(
                    text = "No gear saved yet. Start with the bodies, lenses and accessories you rely on most.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items, key = { it.id }) { item ->
            GearItemCard(
                item = item,
                onEdit = {
                    editing = item
                    showEditor = true
                },
                onDelete = { viewModel.delete(item) },
            )
        }
    }
}

@Composable
private fun GearEditorCard(
    initial: GearItemEntity?,
    onSave: (
        id: Long,
        category: String,
        brand: String,
        model: String,
        serial: String,
        purchaseDate: String,
        purchasePrice: Double?,
        currentValue: Double?,
        weightGrams: Double?,
        notes: String,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: gearCategories.last()) }
    var brand by remember(initial?.id) { mutableStateOf(initial?.brand ?: "") }
    var model by remember(initial?.id) { mutableStateOf(initial?.model ?: "") }
    var serial by remember(initial?.id) { mutableStateOf(initial?.serialNumber ?: "") }
    var purchaseDate by remember(initial?.id) { mutableStateOf(initial?.purchaseDateText ?: "") }
    var purchasePrice by remember(initial?.id) { mutableStateOf(initial?.purchasePrice?.toString() ?: "") }
    var currentValue by remember(initial?.id) { mutableStateOf(initial?.currentValue?.toString() ?: "") }
    var weightGrams by remember(initial?.id) { mutableStateOf(initial?.weightGrams?.toString() ?: "") }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (initial == null) "Add gear item" else "Edit gear item",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                gearCategories.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Brand",
                    value = brand,
                    onValueChange = { brand = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Model",
                    value = model,
                    onValueChange = { model = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Serial",
                    value = serial,
                    onValueChange = { serial = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Purchase date",
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Purchase price",
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                )
                LabeledField(
                    label = "Current value",
                    value = currentValue,
                    onValueChange = { currentValue = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                )
            }
            LabeledField(
                label = "Weight",
                value = weightGrams,
                onValueChange = { weightGrams = it },
                suffix = "g",
                keyboardType = KeyboardType.Decimal,
            )
            LabeledField(
                label = "Notes",
                value = notes,
                onValueChange = { notes = it },
                keyboardType = KeyboardType.Text,
                singleLine = false,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                OutlinedButton(
                    onClick = {
                        onSave(
                            initial?.id ?: 0L,
                            category,
                            brand,
                            model,
                            serial,
                            purchaseDate,
                            purchasePrice.toDoubleOrNull(),
                            currentValue.toDoubleOrNull(),
                            weightGrams.toDoubleOrNull(),
                            notes,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = model.isNotBlank(),
                ) {
                    Text(if (initial == null) "Save" else "Update")
                }
            }
        }
    }
}

@Composable
private fun GearItemCard(
    item: GearItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName(item),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
            if (item.serialNumber.isNotBlank()) {
                Text(
                    text = "Serial: ${item.serialNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val detailLine = buildList {
                if (item.purchaseDateText.isNotBlank()) add("Bought ${item.purchaseDateText}")
                item.currentValue?.let { add("Value ${formatMoney(it)}") }
                item.weightGrams?.let { add(formatWeight(it)) }
            }.joinToString(" · ")
            if (detailLine.isNotBlank()) {
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun displayName(item: GearItemEntity): String =
    listOf(item.brand.trim(), item.model.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { item.model }

private fun formatMoney(value: Double): String = String.format(Locale.US, "$%.2f", value)

private fun formatWeight(grams: Double): String = when {
    grams >= 1000.0 -> String.format(Locale.US, "%.2f kg", grams / 1000.0)
    else -> String.format(Locale.US, "%.0f g", grams)
}
