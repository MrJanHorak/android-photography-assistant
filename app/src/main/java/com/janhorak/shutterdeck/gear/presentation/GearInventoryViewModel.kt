package com.janhorak.shutterdeck.gear.presentation

import android.content.Context
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
import com.janhorak.shutterdeck.core.data.db.GearMaintenanceDao
import com.janhorak.shutterdeck.core.data.db.GearMaintenanceEntryEntity
import com.janhorak.shutterdeck.core.data.db.GearMemoryCardDao
import com.janhorak.shutterdeck.core.data.db.GearMemoryCardEntity
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    @ApplicationContext private val appContext: Context,
    private val gearItemDao: GearItemDao,
    private val gearFilterDao: GearFilterDao,
    private val gearBatteryDao: GearBatteryDao,
    private val gearMemoryCardDao: GearMemoryCardDao,
    private val gearKitDao: GearKitDao,
    private val gearMaintenanceDao: GearMaintenanceDao,
) : ViewModel() {

    private val _seedStatus = MutableStateFlow<String?>(null)
    val seedStatus: StateFlow<String?> = _seedStatus.asStateFlow()

    val items: StateFlow<List<GearItemEntity>> = gearItemDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        id: Long,
        category: String,
        brand: String,
        model: String,
        catalogId: String?,
        filterThreadSizeText: String,
        serialNumber: String,
        purchaseDateText: String,
        purchasePrice: Double?,
        currentValue: Double?,
        weightGrams: Double?,
        notes: String,
    ) {
        val trimmedModel = model.trim()
        if (trimmedModel.isEmpty()) return
        viewModelScope.launch {
            gearItemDao.upsert(
                GearItemEntity(
                    id = id,
                    category = category.ifBlank { "Accessory" },
                    brand = brand.trim(),
                    model = trimmedModel,
                    catalogId = catalogId,
                    filterThreadSizeText = filterThreadSizeText.trim(),
                    serialNumber = serialNumber.trim(),
                    purchaseDateText = purchaseDateText.trim(),
                    purchasePrice = purchasePrice,
                    currentValue = currentValue,
                    weightGrams = weightGrams,
                    notes = notes.trim(),
                ),
            )
        }
    }

    fun delete(item: GearItemEntity) {
        viewModelScope.launch { gearItemDao.delete(item) }
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

private val filterTypeOrder = filterTypeOptions.withIndex().associate { it.value to it.index }
private val batteryStatusOrder = batteryStatusOptions.withIndex().associate { it.value to it.index }
private val memoryCardStatusOrder = memoryCardStatusOptions.withIndex().associate { it.value to it.index }

private fun filterTypeRank(type: String): Int = filterTypeOrder[type] ?: filterTypeOrder.size

private fun batteryStatusRank(status: String): Int = batteryStatusOrder[status] ?: batteryStatusOrder.size

private fun memoryCardStatusRank(status: String): Int = memoryCardStatusOrder[status] ?: memoryCardStatusOrder.size
