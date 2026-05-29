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
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.RadioButton
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
import com.janhorak.shutterdeck.core.data.db.GearMaintenanceEntryEntity
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

private val gearCategories = listOf("Body", "Lens", "Accessory")
private val maintenanceEventTypes = listOf("Cleaning", "Firmware", "Repair", "Shutter count", "Note")

@Composable
fun GearInventoryScreen(
    modifier: Modifier = Modifier,
    viewModel: GearInventoryViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val batteries by viewModel.batteries.collectAsStateWithLifecycle()
    val memoryCards by viewModel.memoryCards.collectAsStateWithLifecycle()
    val kits by viewModel.kits.collectAsStateWithLifecycle()
    val maintenanceEntries by viewModel.maintenanceEntries.collectAsStateWithLifecycle()
    val seedStatus by viewModel.seedStatus.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<GearItemEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editingBattery by remember { mutableStateOf<GearBatterySummary?>(null) }
    var showBatteryEditor by remember { mutableStateOf(false) }
    var editingMemoryCard by remember { mutableStateOf<MemoryCardSummary?>(null) }
    var showMemoryCardEditor by remember { mutableStateOf(false) }
    var editingKit by remember { mutableStateOf<GearKitSummary?>(null) }
    var showKitEditor by remember { mutableStateOf(false) }
    var editingMaintenance by remember { mutableStateOf<GearMaintenanceEntrySummary?>(null) }
    var showMaintenanceEditor by remember { mutableStateOf(false) }

    val bodyCount = items.count { it.category == "Body" }
    val lensCount = items.count { it.category == "Lens" }
    val accessoryCount = items.count { it.category == "Accessory" }
    val catalogLinkedCount = items.count { it.catalogId != null }
    val readyBatteryCount = batteries.count { it.battery.status == "Ready" }
    val needsChargeBatteryCount = batteries.count { it.battery.status == "Needs charge" }
    val emptyCardCount = memoryCards.count { it.card.status == "Empty" }
    val fullCardCount = memoryCards.count { it.card.status == "Full" }
    val totalWeight = items.sumOf { it.weightGrams ?: 0.0 }
    val totalValue = items.sumOf { it.currentValue ?: 0.0 }
    val totalBatteryCapacity = batteries.sumOf { it.battery.capacityMah?.toLong() ?: 0L }
    val totalCardCapacity = memoryCards.sumOf { it.card.capacityGb?.toLong() ?: 0L }
    val hasAnyTrackedData = items.isNotEmpty() ||
        batteries.isNotEmpty() ||
        memoryCards.isNotEmpty() ||
        kits.isNotEmpty() ||
        maintenanceEntries.isNotEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Gear inventory",
                subtitle = "Track bodies, lenses, batteries, cards, kits and maintenance in one place.",
            )
        }
        item {
            OutlinedButton(
                onClick = { viewModel.seedFromCurrentCatalog() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Seed bodies & lenses from meter catalog")
            }
        }
        seedStatus?.let { status ->
            item {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    val nextState = !showEditor
                    editing = null
                    showEditor = nextState
                    if (nextState) {
                        editingBattery = null
                        showBatteryEditor = false
                        editingMemoryCard = null
                        showMemoryCardEditor = false
                        editingKit = null
                        showKitEditor = false
                        editingMaintenance = null
                        showMaintenanceEditor = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add gear item")
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    val nextState = !showBatteryEditor
                    editingBattery = null
                    showBatteryEditor = nextState
                    if (nextState) {
                        editing = null
                        showEditor = false
                        editingMemoryCard = null
                        showMemoryCardEditor = false
                        editingKit = null
                        showKitEditor = false
                        editingMaintenance = null
                        showMaintenanceEditor = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add battery / power pack")
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    val nextState = !showMemoryCardEditor
                    editingMemoryCard = null
                    showMemoryCardEditor = nextState
                    if (nextState) {
                        editing = null
                        showEditor = false
                        editingBattery = null
                        showBatteryEditor = false
                        editingKit = null
                        showKitEditor = false
                        editingMaintenance = null
                        showMaintenanceEditor = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add memory card")
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    val nextState = !showKitEditor
                    editingKit = null
                    showKitEditor = nextState
                    if (nextState) {
                        editing = null
                        showEditor = false
                        editingBattery = null
                        showBatteryEditor = false
                        editingMemoryCard = null
                        showMemoryCardEditor = false
                        editingMaintenance = null
                        showMaintenanceEditor = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = items.isNotEmpty(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add packing kit")
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    val nextState = !showMaintenanceEditor
                    editingMaintenance = null
                    showMaintenanceEditor = nextState
                    if (nextState) {
                        editing = null
                        showEditor = false
                        editingBattery = null
                        showBatteryEditor = false
                        editingMemoryCard = null
                        showMemoryCardEditor = false
                        editingKit = null
                        showKitEditor = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = items.isNotEmpty(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add maintenance log")
            }
        }
        if (showEditor) {
            item {
                GearEditorCard(
                    initial = editing,
                    onSave = { id, category, brand, model, catalogId, serial, purchaseDate, purchasePrice, currentValue, weightGrams, notes ->
                        viewModel.save(
                            id = id,
                            category = category,
                            brand = brand,
                            model = model,
                            catalogId = catalogId,
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
        if (showBatteryEditor) {
            item {
                BatteryEditorCard(
                    initial = editingBattery,
                    availableItems = items,
                    onSave = { id, linkedGearItemId, label, capacityMah, healthPercent, chargePercent, status, lastChargedText, lastCheckedText, notes ->
                        viewModel.saveBattery(
                            id = id,
                            linkedGearItemId = linkedGearItemId,
                            label = label,
                            capacityMah = capacityMah,
                            healthPercent = healthPercent,
                            chargePercent = chargePercent,
                            status = status,
                            lastChargedText = lastChargedText,
                            lastCheckedText = lastCheckedText,
                            notes = notes,
                        )
                        editingBattery = null
                        showBatteryEditor = false
                    },
                    onCancel = {
                        editingBattery = null
                        showBatteryEditor = false
                    },
                )
            }
        }
        if (showMemoryCardEditor) {
            item {
                MemoryCardEditorCard(
                    initial = editingMemoryCard,
                    availableItems = items,
                    onSave = { id, linkedGearItemId, label, cardType, capacityGb, speedLabel, status, lastFormattedText, notes ->
                        viewModel.saveMemoryCard(
                            id = id,
                            linkedGearItemId = linkedGearItemId,
                            label = label,
                            cardType = cardType,
                            capacityGb = capacityGb,
                            speedLabel = speedLabel,
                            status = status,
                            lastFormattedText = lastFormattedText,
                            notes = notes,
                        )
                        editingMemoryCard = null
                        showMemoryCardEditor = false
                    },
                    onCancel = {
                        editingMemoryCard = null
                        showMemoryCardEditor = false
                    },
                )
            }
        }
        if (showKitEditor) {
            item {
                GearKitEditorCard(
                    initial = editingKit,
                    availableItems = items,
                    onSave = { id, name, notes, selectedIds ->
                        viewModel.saveKit(
                            id = id,
                            name = name,
                            notes = notes,
                            gearItemIds = selectedIds,
                        )
                        editingKit = null
                        showKitEditor = false
                    },
                    onCancel = {
                        editingKit = null
                        showKitEditor = false
                    },
                )
            }
        }
        if (showMaintenanceEditor) {
            item {
                MaintenanceEditorCard(
                    initial = editingMaintenance,
                    availableItems = items,
                    onSave = { id, gearItemId, eventType, dateText, shutterCount, notes ->
                        viewModel.saveMaintenance(
                            id = id,
                            gearItemId = gearItemId,
                            eventType = eventType,
                            dateText = dateText,
                            shutterCount = shutterCount,
                            notes = notes,
                        )
                        editingMaintenance = null
                        showMaintenanceEditor = false
                    },
                    onCancel = {
                        editingMaintenance = null
                        showMaintenanceEditor = false
                    },
                )
            }
        }
        if (hasAnyTrackedData) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ResultRow("Items", items.size.toString())
                        ResultRow("Bodies", bodyCount.toString())
                        ResultRow("Lenses", lensCount.toString())
                        ResultRow("Accessories", accessoryCount.toString())
                        ResultRow("Catalog-linked", catalogLinkedCount.toString())
                        ResultRow("Batteries", batteries.size.toString())
                        ResultRow("Ready batteries", readyBatteryCount.toString())
                        ResultRow("Needs charge", needsChargeBatteryCount.toString())
                        ResultRow("Memory cards", memoryCards.size.toString())
                        ResultRow("Empty cards", emptyCardCount.toString())
                        ResultRow("Full cards", fullCardCount.toString())
                        ResultRow("Packing kits", kits.size.toString())
                        ResultRow("Maintenance logs", maintenanceEntries.size.toString())
                        ResultRow("Est. value", formatMoney(totalValue))
                        ResultRow("Total weight", formatWeight(totalWeight))
                        ResultRow(
                            "Battery capacity",
                            if (batteries.any { it.battery.capacityMah != null }) formatBatteryCapacityMah(totalBatteryCapacity) else "—",
                        )
                        ResultRow(
                            "Card storage",
                            if (memoryCards.any { it.card.capacityGb != null }) formatStorageCapacityGb(totalCardCapacity) else "—",
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = "Batteries & power",
                subtitle = "Track spare packs, health, charge state and the last time each one was topped up or checked.",
            )
        }
        if (batteries.isEmpty()) {
            item {
                Text(
                    text = "No batteries yet. Add your spares, V-mounts or AA sets now and link them to gear later if you want.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(batteries, key = { it.battery.id }) { battery ->
            BatteryCard(
                summary = battery,
                onEdit = {
                    editingBattery = battery
                    showBatteryEditor = true
                    editing = null
                    showEditor = false
                    editingMemoryCard = null
                    showMemoryCardEditor = false
                    editingKit = null
                    showKitEditor = false
                    editingMaintenance = null
                    showMaintenanceEditor = false
                },
                onDelete = { viewModel.deleteBattery(battery.battery) },
            )
        }
        item {
            SectionHeader(
                title = "Memory cards",
                subtitle = "Track card type, storage, assignment and whether each card is empty, full or safely backed up.",
            )
        }
        if (memoryCards.isEmpty()) {
            item {
                Text(
                    text = "No cards yet. Add your SD, CFexpress or XQD cards here so you can see what is empty, full or ready to format.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(memoryCards, key = { it.card.id }) { card ->
            MemoryCardCard(
                summary = card,
                onEdit = {
                    editingMemoryCard = card
                    showMemoryCardEditor = true
                    editing = null
                    showEditor = false
                    editingBattery = null
                    showBatteryEditor = false
                    editingKit = null
                    showKitEditor = false
                    editingMaintenance = null
                    showMaintenanceEditor = false
                },
                onDelete = { viewModel.deleteMemoryCard(card.card) },
            )
        }
        item {
            SectionHeader(
                title = "Packing kits",
                subtitle = "Build named grab-and-go kits and tick items off before leaving.",
            )
        }
        if (items.isEmpty()) {
            item {
                Text(
                    text = "Add gear items first, then build packing kits from your saved inventory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (items.isNotEmpty() && kits.isEmpty()) {
            item {
                Text(
                    text = "No kits yet. Create a travel, portrait or everyday carry kit and ShutterDeck will total its weight for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(kits, key = { it.kit.id }) { kit ->
            GearKitCard(
                kit = kit,
                onEdit = {
                    editingKit = kit
                    showKitEditor = true
                    editing = null
                    showEditor = false
                    editingBattery = null
                    showBatteryEditor = false
                    editingMemoryCard = null
                    showMemoryCardEditor = false
                    editingMaintenance = null
                    showMaintenanceEditor = false
                },
                onDelete = { viewModel.deleteKit(kit.kit) },
                onTogglePacked = { viewModel.togglePacked(it) },
            )
        }
        item {
            SectionHeader(
                title = "Maintenance log",
                subtitle = "Track cleanings, firmware, repairs and shutter-count updates.",
            )
        }
        if (items.isEmpty()) {
            item {
                Text(
                    text = "Add gear items first, then log maintenance against the items you own.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (items.isNotEmpty() && maintenanceEntries.isEmpty()) {
            item {
                Text(
                    text = "No maintenance entries yet. Log a sensor cleaning, firmware update, repair or shutter-count checkpoint.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(maintenanceEntries, key = { it.entry.id }) { entry ->
            MaintenanceEntryCard(
                entry = entry,
                onEdit = {
                    editingMaintenance = entry
                    showMaintenanceEditor = true
                    editing = null
                    showEditor = false
                    editingBattery = null
                    showBatteryEditor = false
                    editingMemoryCard = null
                    showMemoryCardEditor = false
                    editingKit = null
                    showKitEditor = false
                },
                onDelete = { viewModel.deleteMaintenance(entry.entry) },
            )
        }
        item {
            SectionHeader(
                title = "Inventory items",
                subtitle = "Everything currently tracked in your gear inventory.",
            )
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
                    editingBattery = null
                    showBatteryEditor = false
                    editingMemoryCard = null
                    showMemoryCardEditor = false
                    editingKit = null
                    showKitEditor = false
                    editingMaintenance = null
                    showMaintenanceEditor = false
                },
                onDelete = { viewModel.delete(item) },
            )
        }
    }
}

@Composable
private fun MaintenanceEditorCard(
    initial: GearMaintenanceEntrySummary?,
    availableItems: List<GearItemEntity>,
    onSave: (Long, Long, String, String, Long?, String) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedItemId by remember(initial?.entry?.id) {
        mutableStateOf(initial?.itemId ?: availableItems.firstOrNull()?.id ?: 0L)
    }
    var eventType by remember(initial?.entry?.id) {
        mutableStateOf(initial?.entry?.eventType ?: maintenanceEventTypes.first())
    }
    var dateText by remember(initial?.entry?.id) { mutableStateOf(initial?.entry?.dateText ?: "") }
    var shutterCount by remember(initial?.entry?.id) { mutableStateOf(initial?.entry?.shutterCount?.toString() ?: "") }
    var notes by remember(initial?.entry?.id) { mutableStateOf(initial?.entry?.notes ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (initial == null) "Add maintenance log" else "Edit maintenance log",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Gear item",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            availableItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedItemId == item.id,
                        onClick = { selectedItemId = item.id },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = gearDisplayName(item),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                maintenanceEventTypes.chunked(3).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { option ->
                            FilterChip(
                                selected = eventType == option,
                                onClick = { eventType = option },
                                label = { Text(option) },
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Date",
                    value = dateText,
                    onValueChange = { dateText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Shutter count",
                    value = shutterCount,
                    onValueChange = { shutterCount = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                )
            }
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
                            initial?.entry?.id ?: 0L,
                            selectedItemId,
                            eventType,
                            dateText,
                            shutterCount.toLongOrNull(),
                            notes,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedItemId != 0L,
                ) {
                    Text(if (initial == null) "Save" else "Update")
                }
            }
        }
    }
}

@Composable
private fun GearKitEditorCard(
    initial: GearKitSummary?,
    availableItems: List<GearItemEntity>,
    onSave: (Long, String, String, List<Long>) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(initial?.kit?.id) { mutableStateOf(initial?.kit?.name ?: "") }
    var notes by remember(initial?.kit?.id) { mutableStateOf(initial?.kit?.notes ?: "") }
    var selectedIds by remember(initial?.kit?.id) {
        mutableStateOf(initial?.items?.map { it.itemId }?.toSet() ?: emptySet())
    }
    val selectedWeight = availableItems
        .filter { it.id in selectedIds }
        .sumOf { it.weightGrams ?: 0.0 }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (initial == null) "Add packing kit" else "Edit packing kit",
                style = MaterialTheme.typography.titleMedium,
            )
            LabeledField(
                label = "Kit name",
                value = name,
                onValueChange = { name = it },
                keyboardType = KeyboardType.Text,
            )
            LabeledField(
                label = "Notes",
                value = notes,
                onValueChange = { notes = it },
                keyboardType = KeyboardType.Text,
                singleLine = false,
            )
            Text(
                text = "Selected items: ${selectedIds.size} · ${formatWeight(selectedWeight)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (availableItems.isEmpty()) {
                Text(
                    text = "Add inventory items before creating a packing kit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                availableItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.id in selectedIds,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) {
                                    selectedIds + item.id
                                } else {
                                    selectedIds - item.id
                                }
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gearDisplayName(item),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = buildList {
                                    add(item.category)
                                    item.weightGrams?.let { add(formatWeight(it)) }
                                }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
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
                            initial?.kit?.id ?: 0L,
                            name,
                            notes,
                            selectedIds.toList(),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && selectedIds.isNotEmpty(),
                ) {
                    Text(if (initial == null) "Save" else "Update")
                }
            }
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
        catalogId: String?,
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
                            initial?.catalogId,
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
private fun GearKitCard(
    kit: GearKitSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePacked: (com.janhorak.shutterdeck.core.data.db.GearKitItemEntity) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kit.kit.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${kit.packedCount}/${kit.items.size} packed · ${formatWeight(kit.totalWeightGrams)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
            if (kit.kit.notes.isNotBlank()) {
                Text(
                    text = kit.kit.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            kit.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = item.entry.packed,
                        onCheckedChange = { onTogglePacked(item.entry) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.itemLabel,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = item.itemWeightGrams?.let(::formatWeight) ?: "Weight not set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceEntryCard(
    entry: GearMaintenanceEntrySummary,
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
                        text = "${entry.itemLabel} · ${entry.entry.eventType}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = buildList {
                            if (entry.entry.dateText.isNotBlank()) add(entry.entry.dateText)
                            entry.entry.shutterCount?.let { add("Shutter $it") }
                        }.joinToString(" · ").ifBlank { "Undated entry" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
            if (entry.entry.notes.isNotBlank()) {
                Text(
                    text = entry.entry.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
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
                        text = gearDisplayName(item),
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
                if (item.catalogId != null) add("Meter catalog")
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
