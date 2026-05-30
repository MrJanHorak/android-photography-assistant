package com.janhorak.shutterdeck.gear.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.GearItemEntity
import com.janhorak.shutterdeck.core.data.db.GearMaintenanceEntryEntity
import com.janhorak.shutterdeck.gear.domain.defaultGearInsuranceCsvFileName
import com.janhorak.shutterdeck.gear.domain.defaultGearInsurancePdfFileName
import com.janhorak.shutterdeck.gear.domain.GearLoanReminderLevel
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
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val lensThreadCompatibility by viewModel.lensThreadCompatibility.collectAsStateWithLifecycle()
    val batteries by viewModel.batteries.collectAsStateWithLifecycle()
    val memoryCards by viewModel.memoryCards.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val insuranceSummary by viewModel.insuranceSummary.collectAsStateWithLifecycle()
    val kits by viewModel.kits.collectAsStateWithLifecycle()
    val maintenanceEntries by viewModel.maintenanceEntries.collectAsStateWithLifecycle()
    val seedStatus by viewModel.seedStatus.collectAsStateWithLifecycle()
    val inventoryStatus by viewModel.inventoryStatus.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
    var editingGearId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingReferencePhotoUri by rememberSaveable { mutableStateOf("") }
    var editingFilter by remember { mutableStateOf<GearFilterSummary?>(null) }
    var showFilterEditor by remember { mutableStateOf(false) }
    var editingBattery by remember { mutableStateOf<GearBatterySummary?>(null) }
    var showBatteryEditor by remember { mutableStateOf(false) }
    var editingMemoryCard by remember { mutableStateOf<MemoryCardSummary?>(null) }
    var showMemoryCardEditor by remember { mutableStateOf(false) }
    var editingLoan by remember { mutableStateOf<GearLoanSummary?>(null) }
    var showLoanEditor by remember { mutableStateOf(false) }
    var editingKit by remember { mutableStateOf<GearKitSummary?>(null) }
    var showKitEditor by remember { mutableStateOf(false) }
    var editingMaintenance by remember { mutableStateOf<GearMaintenanceEntrySummary?>(null) }
    var showMaintenanceEditor by remember { mutableStateOf(false) }
    val editing = editingGearId?.let { editingId -> items.firstOrNull { it.id == editingId } }
    val referencePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            editingReferencePhotoUri = uri.toString()
        }
    }
    val insuranceCsvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportInsuranceCsv(uri)
        }
    }
    val insurancePdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportInsurancePdf(uri)
        }
    }

    val bodyCount = items.count { it.category == "Body" }
    val lensCount = items.count { it.category == "Lens" }
    val accessoryCount = items.count { it.category == "Accessory" }
    val catalogLinkedCount = items.count { it.catalogId != null }
    val itemsWithConditionCount = items.count { it.conditionLabel.isNotBlank() }
    val itemsNeedingServiceCount = items.count { it.conditionLabel == "Needs service" }
    val itemsWithStorageLocationCount = items.count { it.storageLocation.isNotBlank() }
    val itemsWithPurchaseSourceCount = items.count { it.purchaseSource.isNotBlank() }
    val itemsWithReferencePhotoCount = items.count { it.referencePhotoUri.isNotBlank() }
    val lensesWithThreadSizeCount = lensThreadCompatibility.count { it.normalizedThreadKey != null }
    val lensesWithoutThreadSizeCount = lensThreadCompatibility.count { it.normalizedThreadKey == null }
    val lensesWithoutMatchingFiltersCount = lensThreadCompatibility.count {
        it.normalizedThreadKey != null && it.compatibleFilterLabels.isEmpty()
    }
    val filtersWithoutMatchingLensesCount = filters.count {
        it.normalizedThreadKey != null && it.compatibleLensLabels.isEmpty()
    }
    val readyBatteryCount = batteries.count { it.battery.status == "Ready" }
    val needsChargeBatteryCount = batteries.count { it.battery.status == "Needs charge" }
    val emptyCardCount = memoryCards.count { it.card.status == "Empty" }
    val fullCardCount = memoryCards.count { it.card.status == "Full" }
    val openLoanCount = loans.count { it.loan.status == "Active" }
    val dueSoonLoanCount = loans.count {
        it.reminder?.level == GearLoanReminderLevel.UPCOMING ||
            it.reminder?.level == GearLoanReminderLevel.DUE_TODAY
    }
    val overdueLoanCount = loans.count { it.reminder?.level == GearLoanReminderLevel.OVERDUE }
    val totalWeight = items.sumOf { it.weightGrams ?: 0.0 }
    val totalValue = items.sumOf { it.currentValue ?: 0.0 }
    val totalBatteryCapacity = batteries.sumOf { it.battery.capacityMah?.toLong() ?: 0L }
    val totalCardCapacity = memoryCards.sumOf { it.card.capacityGb?.toLong() ?: 0L }
    val hasAnyTrackedData = items.isNotEmpty() ||
        filters.isNotEmpty() ||
        batteries.isNotEmpty() ||
        memoryCards.isNotEmpty() ||
        loans.isNotEmpty() ||
        kits.isNotEmpty() ||
        maintenanceEntries.isNotEmpty()
    val closeAllEditors = {
        editingGearId = null
        showEditor = false
        editingReferencePhotoUri = ""
        editingFilter = null
        showFilterEditor = false
        editingBattery = null
        showBatteryEditor = false
        editingMemoryCard = null
        showMemoryCardEditor = false
        editingLoan = null
        showLoanEditor = false
        editingKit = null
        showKitEditor = false
        editingMaintenance = null
        showMaintenanceEditor = false
        viewModel.clearInventoryStatus()
    }
    val openGearEditor: (GearItemEntity?) -> Unit = { item ->
        closeAllEditors()
        editingGearId = item?.id
        editingReferencePhotoUri = item?.referencePhotoUri.orEmpty()
        showEditor = true
    }
    val openFilterEditor: (GearFilterSummary?) -> Unit = { filter ->
        closeAllEditors()
        editingFilter = filter
        showFilterEditor = true
    }
    val openBatteryEditor: (GearBatterySummary?) -> Unit = { battery ->
        closeAllEditors()
        editingBattery = battery
        showBatteryEditor = true
    }
    val openMemoryCardEditor: (MemoryCardSummary?) -> Unit = { card ->
        closeAllEditors()
        editingMemoryCard = card
        showMemoryCardEditor = true
    }
    val openLoanEditor: (GearLoanSummary?) -> Unit = { loan ->
        closeAllEditors()
        editingLoan = loan
        showLoanEditor = true
    }
    val openKitEditor: (GearKitSummary?) -> Unit = { kit ->
        closeAllEditors()
        editingKit = kit
        showKitEditor = true
    }
    val openMaintenanceEditor: (GearMaintenanceEntrySummary?) -> Unit = { entry ->
        closeAllEditors()
        editingMaintenance = entry
        showMaintenanceEditor = true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Gear inventory",
                subtitle = "Track bodies, lenses, filters, batteries, cards, loans, kits and maintenance in one place.",
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
        inventoryStatus?.let { status ->
            item {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = { openGearEditor(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add gear item")
            }
        }
        item {
            OutlinedButton(
                onClick = { openFilterEditor(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add filter")
            }
        }
        item {
            OutlinedButton(
                onClick = { openBatteryEditor(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add battery / power pack")
            }
        }
        item {
            OutlinedButton(
                onClick = { openMemoryCardEditor(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add memory card")
            }
        }
        item {
            OutlinedButton(
                onClick = { openLoanEditor(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add loan / rental")
            }
        }
        item {
            OutlinedButton(
                onClick = { openKitEditor(null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = items.isNotEmpty(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add packing kit")
            }
        }
        item {
            OutlinedButton(
                onClick = { openMaintenanceEditor(null) },
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
                    referencePhotoUri = editingReferencePhotoUri,
                    onPickReferencePhoto = {
                        referencePhotoPickerLauncher.launch(arrayOf("image/*"))
                    },
                    onClearReferencePhoto = { editingReferencePhotoUri = "" },
                    onSave = { draft ->
                        viewModel.save(draft) { closeAllEditors() }
                    },
                    onCancel = closeAllEditors,
                )
            }
        }
        if (showFilterEditor) {
            item {
                FilterEditorCard(
                    initial = editingFilter,
                    onSave = { id, label, filterType, threadSizeText, strengthText, notes ->
                        viewModel.saveFilter(
                            id = id,
                            label = label,
                            filterType = filterType,
                            threadSizeText = threadSizeText,
                            strengthText = strengthText,
                            notes = notes,
                        )
                        closeAllEditors()
                    },
                    onCancel = closeAllEditors,
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
                        closeAllEditors()
                    },
                    onCancel = closeAllEditors,
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
                        closeAllEditors()
                    },
                    onCancel = closeAllEditors,
                )
            }
        }
        if (showLoanEditor) {
            item {
                GearLoanEditorCard(
                    initial = editingLoan,
                    availableItems = items,
                    onSave = { id, linkedGearItemId, customItemLabel, direction, counterpartName, status, startDateText, dueDateText, returnedDateText, notes ->
                        viewModel.saveLoan(
                            id = id,
                            linkedGearItemId = linkedGearItemId,
                            customItemLabel = customItemLabel,
                            direction = direction,
                            counterpartName = counterpartName,
                            status = status,
                            startDateText = startDateText,
                            dueDateText = dueDateText,
                            returnedDateText = returnedDateText,
                            notes = notes,
                        )
                        closeAllEditors()
                    },
                    onCancel = closeAllEditors,
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
                        closeAllEditors()
                    },
                    onCancel = closeAllEditors,
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
                        closeAllEditors()
                    },
                    onCancel = closeAllEditors,
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
                        ResultRow("Condition saved", itemsWithConditionCount.toString())
                        ResultRow("Needs service", itemsNeedingServiceCount.toString())
                        ResultRow("Storage tracked", itemsWithStorageLocationCount.toString())
                        ResultRow("Purchase source saved", itemsWithPurchaseSourceCount.toString())
                        ResultRow("Reference photos", itemsWithReferencePhotoCount.toString())
                        ResultRow("Filters", filters.size.toString())
                        ResultRow("Lenses with thread size", lensesWithThreadSizeCount.toString())
                        ResultRow("Lenses missing thread size", lensesWithoutThreadSizeCount.toString())
                        ResultRow("Lenses without filters", lensesWithoutMatchingFiltersCount.toString())
                        ResultRow("Filters without lenses", filtersWithoutMatchingLensesCount.toString())
                        ResultRow("Batteries", batteries.size.toString())
                        ResultRow("Ready batteries", readyBatteryCount.toString())
                        ResultRow("Needs charge", needsChargeBatteryCount.toString())
                        ResultRow("Memory cards", memoryCards.size.toString())
                        ResultRow("Empty cards", emptyCardCount.toString())
                        ResultRow("Full cards", fullCardCount.toString())
                        ResultRow("Loans / rentals", loans.size.toString())
                        ResultRow("Open loans / rentals", openLoanCount.toString())
                        ResultRow("Due soon", dueSoonLoanCount.toString())
                        ResultRow("Overdue", overdueLoanCount.toString())
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
                title = "Insurance & export",
                subtitle = "Save a CSV or PDF inventory report for insurance records or value reviews.",
            )
        }
        item {
            GearInsuranceExportCard(
                summary = insuranceSummary,
                exportStatus = exportStatus,
                hasInventoryItems = items.isNotEmpty(),
                onExportCsv = {
                    viewModel.clearExportStatus()
                    insuranceCsvExportLauncher.launch(defaultGearInsuranceCsvFileName())
                },
                onExportPdf = {
                    viewModel.clearExportStatus()
                    insurancePdfExportLauncher.launch(defaultGearInsurancePdfFileName())
                },
            )
        }
        item {
            SectionHeader(
                title = "Filters & fit",
                subtitle = "Track each filter you own and match it against saved lens thread sizes.",
            )
        }
        if (filters.isEmpty()) {
            item {
                Text(
                    text = "No filters yet. Save your ND, CPL, UV or diffusion filters here and ShutterDeck will match them to your lenses by thread size.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(filters, key = { it.filter.id }) { filter ->
            FilterCard(
                summary = filter,
                onEdit = { openFilterEditor(filter) },
                onDelete = { viewModel.deleteFilter(filter.filter) },
            )
        }
        item {
            SectionHeader(
                title = "Lens thread compatibility",
                subtitle = "See which saved lenses still need a thread size and which ones already have matching filters.",
            )
        }
        if (lensThreadCompatibility.isEmpty()) {
            item {
                Text(
                    text = "No lenses yet. Save a lens first, then add its filter thread size so ShutterDeck can show what fits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(lensThreadCompatibility, key = { it.lens.id }) { lens ->
            LensThreadCompatibilityCard(
                summary = lens,
                onEdit = { openGearEditor(lens.lens) },
            )
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
                onEdit = { openBatteryEditor(battery) },
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
                onEdit = { openMemoryCardEditor(card) },
                onDelete = { viewModel.deleteMemoryCard(card.card) },
            )
        }
        item {
            SectionHeader(
                title = "Loans & rentals",
                subtitle = "Track who has your gear, what you borrowed or rented, and what is due back soon.",
            )
        }
        if (loans.isEmpty()) {
            item {
                Text(
                    text = "No loan or rental entries yet. Log borrowed bodies, rented lenses or gear you lent to someone else and save a due date for reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(loans, key = { it.loan.id }) { loan ->
            GearLoanCard(
                summary = loan,
                onEdit = { openLoanEditor(loan) },
                onDelete = { viewModel.deleteLoan(loan.loan) },
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
                onEdit = { openKitEditor(kit) },
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
                onEdit = { openMaintenanceEditor(entry) },
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
                onEdit = { openGearEditor(item) },
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
    referencePhotoUri: String,
    onPickReferencePhoto: () -> Unit,
    onClearReferencePhoto: () -> Unit,
    onSave: (GearItemEditState) -> Unit,
    onCancel: () -> Unit,
) {
    var category by rememberSaveable(initial?.id) { mutableStateOf(initial?.category ?: gearCategories.last()) }
    var brand by rememberSaveable(initial?.id) { mutableStateOf(initial?.brand ?: "") }
    var model by rememberSaveable(initial?.id) { mutableStateOf(initial?.model ?: "") }
    var filterThreadSizeText by rememberSaveable(initial?.id) { mutableStateOf(initial?.filterThreadSizeText ?: "") }
    var conditionLabel by rememberSaveable(initial?.id) { mutableStateOf(initial?.conditionLabel ?: "") }
    var storageLocation by rememberSaveable(initial?.id) { mutableStateOf(initial?.storageLocation ?: "") }
    var purchaseSource by rememberSaveable(initial?.id) { mutableStateOf(initial?.purchaseSource ?: "") }
    var serial by rememberSaveable(initial?.id) { mutableStateOf(initial?.serialNumber ?: "") }
    var purchaseDate by rememberSaveable(initial?.id) { mutableStateOf(initial?.purchaseDateText ?: "") }
    var purchasePrice by rememberSaveable(initial?.id) { mutableStateOf(initial?.purchasePrice?.toString() ?: "") }
    var currentValue by rememberSaveable(initial?.id) { mutableStateOf(initial?.currentValue?.toString() ?: "") }
    var weightGrams by rememberSaveable(initial?.id) { mutableStateOf(initial?.weightGrams?.toString() ?: "") }
    var notes by rememberSaveable(initial?.id) { mutableStateOf(initial?.notes ?: "") }

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
            if (category == "Lens") {
                LabeledField(
                    label = "Filter thread size",
                    value = filterThreadSizeText,
                    onValueChange = { filterThreadSizeText = it },
                    keyboardType = KeyboardType.Text,
                )
                Text(
                    text = "Examples: 67, 67mm, 82 mm or 100mm system",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Condition",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = conditionLabel.isBlank(),
                        onClick = { conditionLabel = "" },
                        label = { Text("Unspecified") },
                    )
                }
                gearConditionOptions.chunked(2).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { option ->
                            FilterChip(
                                selected = conditionLabel == option,
                                onClick = { conditionLabel = option },
                                label = { Text(option) },
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Storage location",
                    value = storageLocation,
                    onValueChange = { storageLocation = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Purchase source",
                    value = purchaseSource,
                    onValueChange = { purchaseSource = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            Text(
                text = "Reference photo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (referencePhotoUri.isBlank()) {
                    "No reference photo attached yet."
                } else {
                    "Attached: ${referencePhotoLabel(referencePhotoUri)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickReferencePhoto,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (referencePhotoUri.isBlank()) "Choose photo" else "Replace photo")
                }
                if (referencePhotoUri.isNotBlank()) {
                    OutlinedButton(
                        onClick = onClearReferencePhoto,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Clear photo")
                    }
                }
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
                            GearItemEditState(
                                id = initial?.id ?: 0L,
                                category = category,
                                brand = brand,
                                model = model,
                                catalogId = initial?.catalogId,
                                filterThreadSizeText = if (category == "Lens") filterThreadSizeText else "",
                                conditionLabel = conditionLabel,
                                storageLocation = storageLocation,
                                purchaseSource = purchaseSource,
                                referencePhotoUri = referencePhotoUri,
                                serialNumber = serial,
                                purchaseDateText = purchaseDate,
                                purchasePrice = purchasePrice.toDoubleOrNull(),
                                currentValue = currentValue.toDoubleOrNull(),
                                weightGrams = weightGrams.toDoubleOrNull(),
                                notes = notes,
                            ),
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
                if (item.category == "Lens" && item.filterThreadSizeText.isNotBlank()) {
                    add("Thread ${formatThreadSizeText(item.filterThreadSizeText)}")
                }
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
            val metadataLine = buildList {
                if (item.conditionLabel.isNotBlank()) add(item.conditionLabel)
                if (item.storageLocation.isNotBlank()) add("Stored ${item.storageLocation}")
                if (item.purchaseSource.isNotBlank()) add("Source ${item.purchaseSource}")
            }.joinToString(" · ")
            if (metadataLine.isNotBlank()) {
                Text(
                    text = metadataLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.referencePhotoUri.isNotBlank()) {
                Text(
                    text = "Reference photo: ${referencePhotoLabel(item.referencePhotoUri)}",
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
