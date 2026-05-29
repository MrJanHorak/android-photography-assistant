package com.janhorak.shutterdeck.metering.presentation

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal enum class GearCatalogSource(val label: String) {
    BUNDLED("Bundled catalog"),
    IMPORTED("Imported catalog"),
    FALLBACK("Built-in fallback"),
}

internal data class GearCatalog(
    val cameraBodyProfiles: List<CameraBodyProfile>,
    val lensProfiles: List<LensProfile>,
    val source: GearCatalogSource,
)

internal data class GearCatalogImportChanges(
    val addedLabels: List<String>,
    val overriddenLabels: List<String>,
) {
    val addedCount: Int
        get() = addedLabels.size

    val overriddenCount: Int
        get() = overriddenLabels.size

    val hasChanges: Boolean
        get() = addedLabels.isNotEmpty() || overriddenLabels.isNotEmpty()
}

internal data class GearCatalogImportPreview(
    val importedJson: String,
    val importedCatalog: GearCatalog,
    val mergedCatalog: GearCatalog,
    val cameraBodyChanges: GearCatalogImportChanges,
    val lensChanges: GearCatalogImportChanges,
) {
    val hasChanges: Boolean
        get() = cameraBodyChanges.hasChanges || lensChanges.hasChanges
}

internal object GearCatalogLoader {
    private const val ASSET_FILE_NAME = "gear_catalog.json"
    private const val IMPORTED_FILE_NAME = "gear_catalog_override.json"

    fun load(context: Context): GearCatalog {
        val bundledCatalog = loadBundledCatalog(context)
        val importedFile = importedCatalogFile(context)

        if (!importedFile.exists()) {
            return bundledCatalog
        }

        val importedCatalog = runCatching {
            parse(importedFile.readText())
        }.getOrNull() ?: return bundledCatalog

        return mergeCatalogs(bundledCatalog, importedCatalog).copy(source = GearCatalogSource.IMPORTED)
    }

    fun importFromUri(context: Context, uri: Uri): Result<GearCatalog> {
        return previewImportFromUri(context, uri).mapCatching { preview ->
            applyImportPreview(context, preview)
        }
    }

    fun previewImportFromUri(context: Context, uri: Uri): Result<GearCatalogImportPreview> {
        return runCatching {
            val importedJson = readCatalogJsonFromUri(context, uri)
            buildImportPreview(
                bundledCatalog = loadBundledCatalog(context),
                importedCatalog = parse(importedJson),
                importedJson = importedJson,
            )
        }
    }

    fun applyImportPreview(context: Context, preview: GearCatalogImportPreview): GearCatalog {
        importedCatalogFile(context).writeText(preview.importedJson)
        return load(context)
    }

    fun clearImportedCatalog(context: Context): GearCatalog {
        importedCatalogFile(context).delete()
        return load(context)
    }

    fun hasImportedCatalog(context: Context): Boolean = importedCatalogFile(context).exists()

    private fun loadBundledCatalog(context: Context): GearCatalog {
        return runCatching {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                parse(reader.readText())
            }
        }.map { parsedCatalog ->
            GearCatalog(
                cameraBodyProfiles = parsedCatalog.cameraBodyProfiles.ifEmpty { cameraBodyProfiles },
                lensProfiles = parsedCatalog.lensProfiles.ifEmpty { lensProfiles },
                source = GearCatalogSource.BUNDLED,
            )
        }.getOrElse {
            GearCatalog(cameraBodyProfiles, lensProfiles, GearCatalogSource.FALLBACK)
        }
    }

    internal fun buildImportPreview(
        bundledCatalog: GearCatalog,
        importedCatalog: GearCatalog,
        importedJson: String,
    ): GearCatalogImportPreview {
        return GearCatalogImportPreview(
            importedJson = importedJson,
            importedCatalog = importedCatalog,
            mergedCatalog = mergeCatalogs(bundledCatalog, importedCatalog),
            cameraBodyChanges = summarizeImportChanges(
                baseItems = bundledCatalog.cameraBodyProfiles,
                importedItems = importedCatalog.cameraBodyProfiles,
                keySelector = { profile -> profile.id },
                labelSelector = { profile -> profile.label },
            ),
            lensChanges = summarizeImportChanges(
                baseItems = bundledCatalog.lensProfiles,
                importedItems = importedCatalog.lensProfiles,
                keySelector = { profile -> profile.id },
                labelSelector = { profile -> profile.label },
            ),
        )
    }

    private fun readCatalogJsonFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readText()
        } ?: error("Unable to read the selected catalog file.")
    }

    private fun parse(json: String): GearCatalog {
        val root = runCatching { JSONObject(json) }
            .getOrElse { throwable ->
                throw IllegalArgumentException("Catalog file is not valid JSON.", throwable)
            }
        val bodyArray = root.optJSONArray("cameraBodyProfiles")
            ?: throw IllegalArgumentException("Catalog JSON must include a cameraBodyProfiles array.")
        val lensArray = root.optJSONArray("lensProfiles")
            ?: throw IllegalArgumentException("Catalog JSON must include a lensProfiles array.")

        if (bodyArray.length() == 0 && lensArray.length() == 0) {
            throw IllegalArgumentException("Catalog JSON must include at least one camera body or lens entry.")
        }

        val parsedCatalog = GearCatalog(
            cameraBodyProfiles = bodyArray.toCameraBodyProfiles(),
            lensProfiles = lensArray.toLensProfiles(),
            source = GearCatalogSource.BUNDLED,
        )
        validateUniqueIds("cameraBodyProfiles", parsedCatalog.cameraBodyProfiles.map { profile -> profile.id })
        validateUniqueIds("lensProfiles", parsedCatalog.lensProfiles.map { profile -> profile.id })
        return parsedCatalog
    }

    internal fun mergeCatalogs(
        bundledCatalog: GearCatalog,
        importedCatalog: GearCatalog,
    ): GearCatalog {
        return GearCatalog(
            cameraBodyProfiles = mergeById(bundledCatalog.cameraBodyProfiles, importedCatalog.cameraBodyProfiles) { profile -> profile.id },
            lensProfiles = mergeById(bundledCatalog.lensProfiles, importedCatalog.lensProfiles) { profile -> profile.id },
            source = GearCatalogSource.IMPORTED,
        )
    }

    private fun <T> summarizeImportChanges(
        baseItems: List<T>,
        importedItems: List<T>,
        keySelector: (T) -> String,
        labelSelector: (T) -> String,
    ): GearCatalogImportChanges {
        val baseIds = baseItems.map(keySelector).toSet()
        val addedLabels = importedItems
            .filter { item -> keySelector(item) !in baseIds }
            .map(labelSelector)
        val overriddenLabels = importedItems
            .filter { item -> keySelector(item) in baseIds }
            .map(labelSelector)
        return GearCatalogImportChanges(
            addedLabels = addedLabels,
            overriddenLabels = overriddenLabels,
        )
    }

    private fun JSONArray.toCameraBodyProfiles(): List<CameraBodyProfile> {
        return List(length()) { index ->
            runCatching { getJSONObject(index).toCameraBodyProfile() }
                .getOrElse { throwable ->
                    throw IllegalArgumentException("cameraBodyProfiles[$index] is invalid: ${throwable.message}", throwable)
                }
        }
    }

    private fun JSONArray.toLensProfiles(): List<LensProfile> {
        return List(length()) { index ->
            runCatching { getJSONObject(index).toLensProfile() }
                .getOrElse { throwable ->
                    throw IllegalArgumentException("lensProfiles[$index] is invalid: ${throwable.message}", throwable)
                }
        }
    }

    private fun JSONObject.toCameraBodyProfile(): CameraBodyProfile {
        val id = getString("id")
        val label = getString("label")
        val nativeMount = LensMount.valueOf(getString("nativeMount"))
        val cropFactor = optDouble("cropFactor", 1.0).toFloat()
        val minIso = getInt("minIso")
        val maxIso = getInt("maxIso")
        val fastestShutterSeconds = getDouble("fastestShutterSeconds")
        val longestStandardShutterSeconds = getDouble("longestStandardShutterSeconds")

        require(id.isNotBlank()) { "id must not be blank." }
        require(label.isNotBlank()) { "label must not be blank." }
        require(cropFactor > 0f) { "cropFactor must be greater than 0." }
        require(minIso > 0) { "minIso must be greater than 0." }
        require(maxIso >= minIso) { "maxIso must be greater than or equal to minIso." }
        require(fastestShutterSeconds > 0.0) { "fastestShutterSeconds must be greater than 0." }
        require(longestStandardShutterSeconds > 0.0) { "longestStandardShutterSeconds must be greater than 0." }
        require(fastestShutterSeconds <= longestStandardShutterSeconds) {
            "fastestShutterSeconds must be less than or equal to longestStandardShutterSeconds."
        }

        return CameraBodyProfile(
            id = id,
            label = label,
            description = getString("description"),
            category = CameraBodyCategory.valueOf(getString("category")),
            cropFactor = cropFactor,
            nativeMount = nativeMount,
            nativeCompatibleMounts = optJSONArray("nativeCompatibleMounts")?.toLensMountSet().orEmpty().ifEmpty {
                setOf(nativeMount)
            },
            adaptedCompatibleMounts = optJSONArray("adaptedCompatibleMounts")?.toLensMountSet().orEmpty(),
            hasInBodyStabilization = optBoolean("hasInBodyStabilization", false),
            minIso = minIso,
            maxIso = maxIso,
            fastestShutterSeconds = fastestShutterSeconds,
            longestStandardShutterSeconds = longestStandardShutterSeconds,
            supportsBulb = optBoolean("supportsBulb", true),
            meteringWorkflowNote = getString("meteringWorkflowNote"),
        )
    }

    private fun JSONObject.toLensProfile(): LensProfile {
        val id = getString("id")
        val label = getString("label")
        val minFocalLengthMm = getInt("minFocalLengthMm")
        val maxFocalLengthMm = optInt("maxFocalLengthMm", minFocalLengthMm)
        val widestApertureAtWideEnd = getDouble("widestApertureAtWideEnd").toFloat()
        val widestApertureAtTeleEnd = optDouble("widestApertureAtTeleEnd", getDouble("widestApertureAtWideEnd")).toFloat()
        val narrowestAperture = getDouble("narrowestAperture").toFloat()

        require(id.isNotBlank()) { "id must not be blank." }
        require(label.isNotBlank()) { "label must not be blank." }
        require(minFocalLengthMm > 0) { "minFocalLengthMm must be greater than 0." }
        require(maxFocalLengthMm >= minFocalLengthMm) { "maxFocalLengthMm must be greater than or equal to minFocalLengthMm." }
        require(widestApertureAtWideEnd > 0f) { "widestApertureAtWideEnd must be greater than 0." }
        require(widestApertureAtTeleEnd > 0f) { "widestApertureAtTeleEnd must be greater than 0." }
        require(narrowestAperture >= widestApertureAtWideEnd && narrowestAperture >= widestApertureAtTeleEnd) {
            "narrowestAperture must be greater than or equal to the widest aperture values."
        }

        return LensProfile(
            id = id,
            label = label,
            description = getString("description"),
            mount = LensMount.valueOf(getString("mount")),
            minFocalLengthMm = minFocalLengthMm,
            maxFocalLengthMm = maxFocalLengthMm,
            widestApertureAtWideEnd = widestApertureAtWideEnd,
            widestApertureAtTeleEnd = widestApertureAtTeleEnd,
            narrowestAperture = narrowestAperture,
            hasOpticalStabilization = optBoolean("hasOpticalStabilization", false),
        )
    }

    private fun JSONArray.toLensMountSet(): Set<LensMount> {
        return List(length()) { index -> LensMount.valueOf(getString(index)) }.toSet()
    }

    private fun importedCatalogFile(context: Context): File = File(context.filesDir, IMPORTED_FILE_NAME)

    private fun validateUniqueIds(collectionName: String, ids: List<String>) {
        val seenIds = mutableSetOf<String>()
        val duplicateId = ids.firstOrNull { id -> !seenIds.add(id) }
        require(duplicateId == null) { "$collectionName contains duplicate id \"$duplicateId\"." }
    }

    private fun <T> mergeById(
        baseItems: List<T>,
        overrideItems: List<T>,
        keySelector: (T) -> String,
    ): List<T> {
        val mergedMap = LinkedHashMap<String, T>()
        baseItems.forEach { item ->
            mergedMap[keySelector(item)] = item
        }
        overrideItems.forEach { item ->
            mergedMap[keySelector(item)] = item
        }
        return mergedMap.values.toList()
    }
}