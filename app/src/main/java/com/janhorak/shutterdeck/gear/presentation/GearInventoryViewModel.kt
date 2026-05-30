package com.janhorak.shutterdeck.gear.presentation

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.GearBatteryDao
import com.janhorak.shutterdeck.core.data.db.GearBatteryEntity
import com.janhorak.shutterdeck.core.data.db.GearFilterDao
import com.janhorak.shutterdeck.core.data.db.GearFilterEntity
import com.janhorak.shutterdeck.core.data.db.GearKitDao
import com.janhorak.shutterdeck.core.data.db.GearKitEntity
import com.janhorak.shutterdeck.core.data.db.GearKitItemEntity
import com.janhorak.shutterdeck.core.data.db.GearItemDao
import com.janhorak.shutterdeck.core.data.db.GearItemEntity
import com.janhorak.shutterdeck.core.data.db.GearLoanDao
import com.janhorak.shutterdeck.core.data.db.GearLoanEntity
import com.janhorak.shutterdeck.core.data.db.GearMaintenanceDao
import com.janhorak.shutterdeck.core.data.db.GearMaintenanceEntryEntity
import com.janhorak.shutterdeck.core.data.db.GearMemoryCardDao
import com.janhorak.shutterdeck.core.data.db.GearMemoryCardEntity
import com.janhorak.shutterdeck.core.storage.ReferencePhotoGrantManager
import com.janhorak.shutterdeck.gear.domain.GearInsuranceExportItem
import com.janhorak.shutterdeck.gear.domain.GearInsuranceSummary
import com.janhorak.shutterdeck.gear.domain.GearLoanReminder
import com.janhorak.shutterdeck.gear.domain.GearLoanReminderLevel
import com.janhorak.shutterdeck.gear.domain.buildGearInsuranceCsv
import com.janhorak.shutterdeck.gear.domain.buildGearInsuranceExportReport
import com.janhorak.shutterdeck.gear.domain.buildGearInsurancePdfLines
import com.janhorak.shutterdeck.gear.domain.calculateGearLoanReminder
import com.janhorak.shutterdeck.gear.domain.parseGearLoanDate
import com.janhorak.shutterdeck.metering.presentation.CameraBodyProfile
import com.janhorak.shutterdeck.metering.presentation.GearCatalogLoader
import com.janhorak.shutterdeck.metering.presentation.LensProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

data class GearKitItemSummary(
    val entry: GearKitItemEntity,
    val itemId: Long,
    val itemLabel: String,
    val itemWeightGrams: Double?,
)

data class GearKitSummary(
    val kit: GearKitEntity,
    val items: List<GearKitItemSummary>,
    val totalWeightGrams: Double,
    val packedCount: Int,
)

data class GearMaintenanceEntrySummary(
    val entry: GearMaintenanceEntryEntity,
    val itemId: Long,
    val itemLabel: String,
)

data class GearBatterySummary(
    val battery: GearBatteryEntity,
    val linkedItemLabel: String,
)

data class MemoryCardSummary(
    val card: GearMemoryCardEntity,
    val linkedItemLabel: String,
)

data class GearLoanSummary(
    val loan: GearLoanEntity,
    val itemLabel: String,
    val linkedItemLabel: String?,
    val reminder: GearLoanReminder?,
)

data class GearFilterSummary(
    val filter: GearFilterEntity,
    val normalizedThreadKey: String?,
    val compatibleLensLabels: List<String>,
)

data class LensThreadCompatibilitySummary(
    val lens: GearItemEntity,
    val normalizedThreadKey: String?,
    val compatibleFilterLabels: List<String>,
)

@HiltViewModel
class GearInventoryViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val gearItemDao: GearItemDao,
    private val referencePhotoGrantManager: ReferencePhotoGrantManager,
    private val gearFilterDao: GearFilterDao,
    private val gearBatteryDao: GearBatteryDao,
    private val gearMemoryCardDao: GearMemoryCardDao,
    private val gearLoanDao: GearLoanDao,
    private val gearKitDao: GearKitDao,
    private val gearMaintenanceDao: GearMaintenanceDao,
) : ViewModel() {

    private val _seedStatus = MutableStateFlow<String?>(null)
    val seedStatus: StateFlow<String?> = _seedStatus.asStateFlow()
    private val _inventoryStatus = MutableStateFlow<String?>(null)
    val inventoryStatus: StateFlow<String?> = _inventoryStatus.asStateFlow()
    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    val items: StateFlow<List<GearItemEntity>> = gearItemDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insuranceSummary: StateFlow<GearInsuranceSummary> = items
        .map { inventoryItems ->
            buildGearInsuranceExportReport(
                inventoryItems.map(::gearInsuranceExportItem)
            ).summary
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            buildGearInsuranceExportReport(emptyList()).summary,
        )

    val filters: StateFlow<List<GearFilterSummary>> = combine(
        items,
        gearFilterDao.observeAll(),
    ) { inventoryItems, filters ->
        val lenses = inventoryItems
            .filter { it.category == "Lens" }
            .map { lens ->
                lens to normalizeThreadSize(lens.filterThreadSizeText)
            }
        filters.map { filter ->
            val filterKey = normalizeThreadSize(filter.threadSizeText)
            val compatibleLensLabels = if (filterKey == null) {
                emptyList()
            } else {
                lenses
                    .filter { (_, lensKey) -> lensKey == filterKey }
                    .map { (lens, _) -> gearDisplayName(lens) }
                    .sortedBy { it.lowercase() }
            }
            GearFilterSummary(
                filter = filter,
                normalizedThreadKey = filterKey,
                compatibleLensLabels = compatibleLensLabels,
            )
        }.sortedWith(
            compareBy<GearFilterSummary> { it.normalizedThreadKey == null }
                .thenBy { it.compatibleLensLabels.isNotEmpty() }
                .thenBy { filterTypeRank(it.filter.filterType) }
                .thenBy { it.filter.label.lowercase() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lensThreadCompatibility: StateFlow<List<LensThreadCompatibilitySummary>> = combine(
        items,
        gearFilterDao.observeAll(),
    ) { inventoryItems, filters ->
        val filterSummaries = filters
            .map { filter -> filter to normalizeThreadSize(filter.threadSizeText) }
        inventoryItems
            .filter { it.category == "Lens" }
            .map { lens ->
                val lensKey = normalizeThreadSize(lens.filterThreadSizeText)
                val compatibleFilterLabels = if (lensKey == null) {
                    emptyList()
                } else {
                    filterSummaries
                        .filter { (_, filterKey) -> filterKey == lensKey }
                        .map { (filter, _) -> filterLabel(filter) }
                        .sortedBy { it.lowercase() }
                }
                LensThreadCompatibilitySummary(
                    lens = lens,
                    normalizedThreadKey = lensKey,
                    compatibleFilterLabels = compatibleFilterLabels,
                )
            }
            .sortedWith(
                compareBy<LensThreadCompatibilitySummary> { it.normalizedThreadKey == null }
                    .thenBy { it.compatibleFilterLabels.isNotEmpty() }
                    .thenBy { gearDisplayName(it.lens).lowercase() },
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batteries: StateFlow<List<GearBatterySummary>> = combine(
        items,
        gearBatteryDao.observeAll(),
    ) { inventoryItems, batteries ->
        val itemMap = inventoryItems.associateBy { it.id }
        batteries.map { battery ->
            GearBatterySummary(
                battery = battery,
                linkedItemLabel = battery.linkedGearItemId
                    ?.let { itemId -> itemMap[itemId]?.let(::gearDisplayName) }
                    ?: UNASSIGNED_GEAR_LABEL,
            )
        }.sortedWith(
            compareBy<GearBatterySummary> { batteryStatusRank(it.battery.status) }
                .thenBy { it.linkedItemLabel.lowercase() }
                .thenBy { it.battery.label.lowercase() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryCards: StateFlow<List<MemoryCardSummary>> = combine(
        items,
        gearMemoryCardDao.observeAll(),
    ) { inventoryItems, cards ->
        val itemMap = inventoryItems.associateBy { it.id }
        cards.map { card ->
            MemoryCardSummary(
                card = card,
                linkedItemLabel = card.linkedGearItemId
                    ?.let { itemId -> itemMap[itemId]?.let(::gearDisplayName) }
                    ?: UNASSIGNED_GEAR_LABEL,
            )
        }.sortedWith(
            compareBy<MemoryCardSummary> { memoryCardStatusRank(it.card.status) }
                .thenBy { it.linkedItemLabel.lowercase() }
                .thenBy { it.card.label.lowercase() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<GearLoanSummary>> = combine(
        items,
        gearLoanDao.observeAll(),
    ) { inventoryItems, loans ->
        val itemMap = inventoryItems.associateBy { it.id }
        loans.map { loan ->
            val linkedItemLabel = loan.linkedGearItemId
                ?.let { itemId -> itemMap[itemId]?.let(::gearDisplayName) }
            val itemLabel = linkedItemLabel
                ?: loan.customItemLabel.trim().ifBlank { "Unnamed gear" }
            GearLoanSummary(
                loan = loan,
                itemLabel = itemLabel,
                linkedItemLabel = linkedItemLabel,
                reminder = calculateGearLoanReminder(
                    status = loan.status,
                    dueDateText = loan.dueDateText,
                ),
            )
        }.sortedWith(
            compareBy<GearLoanSummary> { gearLoanSortRank(it) }
                .thenBy { parseGearLoanDate(it.loan.dueDateText)?.toEpochDay() ?: Long.MAX_VALUE }
                .thenBy { it.itemLabel.lowercase() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val kits: StateFlow<List<GearKitSummary>> = combine(
        items,
        gearKitDao.observeKits(),
        gearKitDao.observeKitItems(),
    ) { inventoryItems, kits, kitItems ->
        val itemMap = inventoryItems.associateBy { it.id }
        kits.map { kit ->
            val summaryItems = kitItems
                .filter { it.kitId == kit.id }
                .mapNotNull { entry ->
                    itemMap[entry.gearItemId]?.let { item ->
                        GearKitItemSummary(
                            entry = entry,
                            itemId = item.id,
                            itemLabel = gearDisplayName(item),
                            itemWeightGrams = item.weightGrams,
                        )
                    }
                }
                .sortedWith(
                    compareBy<GearKitItemSummary> { it.entry.packed }
                        .thenBy { it.itemLabel.lowercase() },
                )
            GearKitSummary(
                kit = kit,
                items = summaryItems,
                totalWeightGrams = summaryItems.sumOf { it.itemWeightGrams ?: 0.0 },
                packedCount = summaryItems.count { it.entry.packed },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceEntries: StateFlow<List<GearMaintenanceEntrySummary>> = combine(
        items,
        gearMaintenanceDao.observeAll(),
    ) { inventoryItems, entries ->
        val itemMap = inventoryItems.associateBy { it.id }
        entries.mapNotNull { entry ->
            itemMap[entry.gearItemId]?.let { item ->
                GearMaintenanceEntrySummary(
                    entry = entry,
                    itemId = item.id,
                    itemLabel = gearDisplayName(item),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(
        draft: GearItemEditState,
        onSuccess: () -> Unit = {},
    ) {
        val trimmedModel = draft.model.trim()
        if (trimmedModel.isEmpty()) return
        viewModelScope.launch {
            val previousReferencePhotoUri = items.value
                .firstOrNull { it.id == draft.id }
                ?.referencePhotoUri
                .orEmpty()
            if (
                !referencePhotoGrantManager.updateGearReferencePhotoGrant(
                    itemId = draft.id,
                    previousUriString = previousReferencePhotoUri,
                    nextUriString = draft.referencePhotoUri,
                )
            ) {
                _inventoryStatus.value = "Couldn't save access to the selected reference photo. Choose it again."
                return@launch
            }
            gearItemDao.upsert(
                GearItemEntity(
                    id = draft.id,
                    category = draft.category.ifBlank { "Accessory" },
                    brand = draft.brand.trim(),
                    model = trimmedModel,
                    catalogId = draft.catalogId,
                    filterThreadSizeText = draft.filterThreadSizeText.trim(),
                    conditionLabel = draft.conditionLabel.trim(),
                    storageLocation = draft.storageLocation.trim(),
                    purchaseSource = draft.purchaseSource.trim(),
                    referencePhotoUri = draft.referencePhotoUri.trim(),
                    serialNumber = draft.serialNumber.trim(),
                    purchaseDateText = draft.purchaseDateText.trim(),
                    purchasePrice = draft.purchasePrice,
                    currentValue = draft.currentValue,
                    weightGrams = draft.weightGrams,
                    notes = draft.notes.trim(),
                ),
            )
            _inventoryStatus.value = null
            onSuccess()
        }
    }

    fun delete(item: GearItemEntity) {
        viewModelScope.launch {
            referencePhotoGrantManager.releaseGearReferencePhotoGrant(
                uriString = item.referencePhotoUri,
                excludingItemId = item.id,
            )
            gearItemDao.delete(item)
        }
    }

    fun clearInventoryStatus() {
        _inventoryStatus.value = null
    }

    fun clearExportStatus() {
        _exportStatus.value = null
    }

    fun saveFilter(
        id: Long,
        label: String,
        filterType: String,
        threadSizeText: String,
        strengthText: String,
        notes: String,
    ) {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) return
        viewModelScope.launch {
            gearFilterDao.upsert(
                GearFilterEntity(
                    id = id,
                    label = trimmedLabel,
                    filterType = filterType.ifBlank { filterTypeOptions.first() },
                    threadSizeText = threadSizeText.trim(),
                    strengthText = strengthText.trim(),
                    notes = notes.trim(),
                ),
            )
        }
    }

    fun deleteFilter(item: GearFilterEntity) {
        viewModelScope.launch { gearFilterDao.delete(item) }
    }

    fun saveBattery(
        id: Long,
        linkedGearItemId: Long?,
        label: String,
        capacityMah: Int?,
        healthPercent: Int?,
        chargePercent: Int?,
        status: String,
        lastChargedText: String,
        lastCheckedText: String,
        notes: String,
    ) {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) return
        viewModelScope.launch {
            gearBatteryDao.upsert(
                GearBatteryEntity(
                    id = id,
                    linkedGearItemId = linkedGearItemId,
                    label = trimmedLabel,
                    capacityMah = capacityMah,
                    healthPercent = healthPercent,
                    chargePercent = chargePercent,
                    status = status.ifBlank { batteryStatusOptions.first() },
                    lastChargedText = lastChargedText.trim(),
                    lastCheckedText = lastCheckedText.trim(),
                    notes = notes.trim(),
                ),
            )
        }
    }

    fun deleteBattery(item: GearBatteryEntity) {
        viewModelScope.launch { gearBatteryDao.delete(item) }
    }

    fun saveMemoryCard(
        id: Long,
        linkedGearItemId: Long?,
        label: String,
        cardType: String,
        capacityGb: Int?,
        speedLabel: String,
        status: String,
        lastFormattedText: String,
        notes: String,
    ) {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) return
        viewModelScope.launch {
            gearMemoryCardDao.upsert(
                GearMemoryCardEntity(
                    id = id,
                    linkedGearItemId = linkedGearItemId,
                    label = trimmedLabel,
                    cardType = cardType.ifBlank { memoryCardTypeOptions.first() },
                    capacityGb = capacityGb,
                    speedLabel = speedLabel.trim(),
                    status = status.ifBlank { memoryCardStatusOptions.first() },
                    lastFormattedText = lastFormattedText.trim(),
                    notes = notes.trim(),
                ),
            )
        }
    }

    fun deleteMemoryCard(item: GearMemoryCardEntity) {
        viewModelScope.launch { gearMemoryCardDao.delete(item) }
    }

    fun saveLoan(
        id: Long,
        linkedGearItemId: Long?,
        customItemLabel: String,
        direction: String,
        counterpartName: String,
        status: String,
        startDateText: String,
        dueDateText: String,
        returnedDateText: String,
        notes: String,
    ) {
        val trimmedCounterpartName = counterpartName.trim()
        val linkedItemFallback = linkedGearItemId
            ?.let { itemId -> items.value.firstOrNull { it.id == itemId }?.let(::gearDisplayName) }
            .orEmpty()
        val trimmedCustomItemLabel = customItemLabel.trim().ifBlank { linkedItemFallback }
        val createdAt = loans.value.firstOrNull { it.loan.id == id }?.loan?.createdAt
            ?: System.currentTimeMillis()
        if (trimmedCustomItemLabel.isEmpty() || trimmedCounterpartName.isEmpty()) return

        viewModelScope.launch {
            gearLoanDao.upsert(
                GearLoanEntity(
                    id = id,
                    linkedGearItemId = linkedGearItemId,
                    customItemLabel = trimmedCustomItemLabel,
                    direction = direction.ifBlank { loanDirectionOptions.first() },
                    counterpartName = trimmedCounterpartName,
                    status = status.ifBlank { loanStatusOptions.first() },
                    startDateText = startDateText.trim(),
                    dueDateText = dueDateText.trim(),
                    returnedDateText = if (status == "Returned") returnedDateText.trim() else "",
                    notes = notes.trim(),
                    createdAt = createdAt,
                ),
            )
        }
    }

    fun deleteLoan(item: GearLoanEntity) {
        viewModelScope.launch { gearLoanDao.delete(item) }
    }

    fun exportInsuranceCsv(destination: Uri) {
        val exportItems = items.value.map(::gearInsuranceExportItem)
        if (exportItems.isEmpty()) {
            _exportStatus.value = "Add at least one saved gear item before exporting."
            return
        }
        viewModelScope.launch {
            try {
                writeInsuranceCsv(destination, exportItems)
                _exportStatus.value = "Saved the CSV insurance export."
            } catch (_: SecurityException) {
                _exportStatus.value = "Couldn't access the selected document."
            } catch (error: IOException) {
                _exportStatus.value = error.message ?: "Couldn't write the CSV insurance export."
            }
        }
    }

    fun exportInsurancePdf(destination: Uri) {
        val exportItems = items.value.map(::gearInsuranceExportItem)
        if (exportItems.isEmpty()) {
            _exportStatus.value = "Add at least one saved gear item before exporting."
            return
        }
        viewModelScope.launch {
            try {
                writeInsurancePdf(destination, exportItems)
                _exportStatus.value = "Saved the PDF insurance export."
            } catch (_: SecurityException) {
                _exportStatus.value = "Couldn't access the selected document."
            } catch (error: IOException) {
                _exportStatus.value = error.message ?: "Couldn't write the PDF insurance export."
            }
        }
    }

    fun saveKit(
        id: Long,
        name: String,
        notes: String,
        gearItemIds: List<Long>,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            gearKitDao.replaceKit(
                kit = GearKitEntity(
                    id = id,
                    name = trimmedName,
                    notes = notes.trim(),
                ),
                gearItemIds = gearItemIds,
            )
        }
    }

    fun deleteKit(kit: GearKitEntity) {
        viewModelScope.launch { gearKitDao.deleteKit(kit) }
    }

    fun togglePacked(item: GearKitItemEntity) {
        viewModelScope.launch {
            gearKitDao.upsertKitItem(item.copy(packed = !item.packed))
        }
    }

    fun saveMaintenance(
        id: Long,
        gearItemId: Long,
        eventType: String,
        dateText: String,
        shutterCount: Long?,
        notes: String,
    ) {
        if (gearItemId == 0L) return
        viewModelScope.launch {
            gearMaintenanceDao.upsert(
                GearMaintenanceEntryEntity(
                    id = id,
                    gearItemId = gearItemId,
                    eventType = eventType.ifBlank { "Cleaning" },
                    dateText = dateText.trim(),
                    shutterCount = shutterCount,
                    notes = notes.trim(),
                ),
            )
        }
    }

    fun deleteMaintenance(entry: GearMaintenanceEntryEntity) {
        viewModelScope.launch { gearMaintenanceDao.delete(entry) }
    }

    fun seedFromCurrentCatalog() {
        viewModelScope.launch {
            val catalog = withContext(Dispatchers.IO) { GearCatalogLoader.load(appContext) }
            val existingItems = items.value
            val existingCatalogIds = existingItems.mapNotNull { it.catalogId }.toSet()
            val existingInventoryKeys = existingItems.map(::inventoryKey).toSet()
            val seeds = buildList {
                addAll(catalog.cameraBodyProfiles.map(::bodySeed))
                addAll(catalog.lensProfiles.map(::lensSeed))
            }
            val missingSeeds = seeds.filter { seed ->
                seed.catalogId !in existingCatalogIds && inventoryKey(seed) !in existingInventoryKeys
            }
            missingSeeds.forEach { seed -> gearItemDao.upsert(seed) }
            _seedStatus.value = if (missingSeeds.isEmpty()) {
                "No new items to seed from the ${catalog.source.label.lowercase()}."
            } else {
                "Added ${missingSeeds.size} item(s) from the ${catalog.source.label.lowercase()}."
            }
        }
    }

    private suspend fun writeInsuranceCsv(
        destination: Uri,
        exportItems: List<GearInsuranceExportItem>,
    ) {
        withContext(Dispatchers.IO) {
            val report = buildGearInsuranceExportReport(exportItems)
            val outputStream = appContext.contentResolver.openOutputStream(destination, "w")
                ?: throw IOException("Couldn't open the selected document for writing.")
            outputStream.bufferedWriter().use { writer ->
                writer.write(buildGearInsuranceCsv(report))
            }
        }
    }

    private suspend fun writeInsurancePdf(
        destination: Uri,
        exportItems: List<GearInsuranceExportItem>,
    ) {
        withContext(Dispatchers.IO) {
            val report = buildGearInsuranceExportReport(exportItems)
            val outputStream = appContext.contentResolver.openOutputStream(destination, "w")
                ?: throw IOException("Couldn't open the selected document for writing.")
            outputStream.use { stream ->
                writePdfDocument(
                    outputStream = stream,
                    lines = buildGearInsurancePdfLines(report),
                )
            }
        }
    }

}

private fun bodySeed(profile: CameraBodyProfile): GearItemEntity {
    val (brand, model) = splitCatalogLabel(profile.label)
    val details = buildList {
        add(profile.description)
        add("${profile.category.label} body")
        add("${profile.nativeMount.label} mount")
        add("${formatOneDecimal(profile.cropFactor.toDouble())}x crop")
        if (profile.hasInBodyStabilization) add("IBIS")
    }
    return GearItemEntity(
        category = "Body",
        brand = brand,
        model = model,
        catalogId = "body:${profile.id}",
        filterThreadSizeText = "",
        serialNumber = "",
        purchaseDateText = "",
        purchasePrice = null,
        currentValue = null,
        weightGrams = null,
        notes = details.joinToString(" · "),
    )
}

private fun lensSeed(profile: LensProfile): GearItemEntity {
    val (brand, model) = splitCatalogLabel(profile.label)
    val details = buildList {
        add(profile.description)
        add(profile.focalLengthRangeLabel)
        add(profile.widestApertureRangeLabel)
        add(profile.mountSummary)
        if (profile.hasOpticalStabilization) add("Optical stabilization")
    }
    return GearItemEntity(
        category = "Lens",
        brand = brand,
        model = model,
        catalogId = "lens:${profile.id}",
        filterThreadSizeText = "",
        serialNumber = "",
        purchaseDateText = "",
        purchasePrice = null,
        currentValue = null,
        weightGrams = null,
        notes = details.joinToString(" · "),
    )
}

private fun splitCatalogLabel(label: String): Pair<String, String> {
    val parts = label.trim().split(Regex("\\s+"), limit = 2)
    return when {
        parts.size >= 2 -> parts[0] to parts[1]
        else -> "" to label.trim()
    }
}

private fun inventoryKey(item: GearItemEntity): String =
    listOf(item.category, item.brand, item.model)
        .joinToString("|") { it.trim().lowercase() }

private fun formatOneDecimal(value: Double): String = "%.1f".format(value)

private fun filterLabel(filter: GearFilterEntity): String =
    listOf(filter.label.trim(), filter.strengthText.trim())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { filter.label }

private fun gearInsuranceExportItem(item: GearItemEntity): GearInsuranceExportItem =
    GearInsuranceExportItem(
        category = item.category.trim().ifBlank { "Accessory" },
        itemName = gearDisplayName(item),
        serialNumber = item.serialNumber.trim(),
        purchaseDateText = item.purchaseDateText.trim(),
        purchaseSource = item.purchaseSource.trim(),
        storageLocation = item.storageLocation.trim(),
        conditionLabel = item.conditionLabel.trim(),
        purchasePrice = item.purchasePrice,
        currentValue = item.currentValue,
        weightGrams = item.weightGrams,
        hasReferencePhoto = item.referencePhotoUri.trim().isNotBlank(),
        notes = item.notes.trim(),
    )

private fun writePdfDocument(
    outputStream: OutputStream,
    lines: List<String>,
) {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val horizontalMargin = 40f
    val topMargin = 48f
    val bottomMargin = 48f
    val contentWidth = pageWidth - (horizontalMargin * 2)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = topMargin

    lines.forEachIndexed { index, line ->
        val paint = if (index == 0) titlePaint else bodyPaint
        val lineHeight = if (paint === titlePaint) 24f else 16f
        val wrappedLines = wrapPdfLine(
            text = line,
            paint = paint,
            maxWidth = contentWidth,
        )
        wrappedLines.forEach { wrappedLine ->
            if (y > pageHeight - bottomMargin) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = topMargin
            }
            if (wrappedLine.isNotEmpty()) {
                canvas.drawText(wrappedLine, horizontalMargin, y, paint)
            }
            y += lineHeight
        }
    }

    try {
        document.finishPage(page)
        document.writeTo(outputStream)
    } finally {
        document.close()
    }
}

private fun wrapPdfLine(
    text: String,
    paint: Paint,
    maxWidth: Float,
): List<String> {
    if (text.isBlank()) return listOf("")

    val segments = mutableListOf<String>()
    var remaining = text.trimEnd()
    while (remaining.isNotEmpty()) {
        if (paint.measureText(remaining) <= maxWidth) {
            segments += remaining
            break
        }

        val candidate = remaining
            .takeWhileMaxWidth(paint, maxWidth)
            .trimEnd()
        val breakIndex = candidate.lastIndexOf(' ')
        val line = when {
            breakIndex > 0 -> candidate.substring(0, breakIndex).trimEnd()
            candidate.isNotEmpty() -> candidate
            else -> remaining.take(1)
        }
        segments += line
        remaining = remaining.removePrefix(line).trimStart()
    }
    return segments
}

private fun String.takeWhileMaxWidth(
    paint: Paint,
    maxWidth: Float,
): String {
    if (isEmpty()) return ""
    var bestIndex = 0
    for (index in indices) {
        val candidate = substring(0, index + 1)
        if (paint.measureText(candidate) <= maxWidth) {
            bestIndex = index + 1
        } else {
            break
        }
    }
    return substring(0, bestIndex)
}

private val filterTypeOrder = filterTypeOptions.withIndex().associate { it.value to it.index }
private val batteryStatusOrder = batteryStatusOptions.withIndex().associate { it.value to it.index }
private val memoryCardStatusOrder = memoryCardStatusOptions.withIndex().associate { it.value to it.index }
private val loanStatusOrder = loanStatusOptions.withIndex().associate { it.value to it.index }

private fun filterTypeRank(type: String): Int = filterTypeOrder[type] ?: filterTypeOrder.size

private fun batteryStatusRank(status: String): Int = batteryStatusOrder[status] ?: batteryStatusOrder.size

private fun memoryCardStatusRank(status: String): Int = memoryCardStatusOrder[status] ?: memoryCardStatusOrder.size

private fun gearLoanStatusRank(status: String): Int = loanStatusOrder[status] ?: loanStatusOrder.size

private fun gearLoanReminderRank(reminder: GearLoanReminder?): Int = when (reminder?.level) {
    GearLoanReminderLevel.OVERDUE -> 0
    GearLoanReminderLevel.DUE_TODAY -> 1
    GearLoanReminderLevel.UPCOMING -> 2
    null -> 3
}

private fun gearLoanSortRank(summary: GearLoanSummary): Int = when (summary.loan.status) {
    "Active" -> gearLoanReminderRank(summary.reminder)
    else -> 10 + gearLoanStatusRank(summary.loan.status)
}
