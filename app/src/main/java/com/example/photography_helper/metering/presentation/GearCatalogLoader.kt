package com.example.photography_helper.metering.presentation

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
        return runCatching {
            val importedJson = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                reader.readText()
            } ?: error("Unable to read the selected catalog file.")

            parse(importedJson)
            importedCatalogFile(context).writeText(importedJson)
            load(context)
        }
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

    private fun parse(json: String): GearCatalog {
        val root = JSONObject(json)
        return GearCatalog(
            cameraBodyProfiles = root.optJSONArray("cameraBodyProfiles")?.toCameraBodyProfiles().orEmpty(),
            lensProfiles = root.optJSONArray("lensProfiles")?.toLensProfiles().orEmpty(),
            source = GearCatalogSource.BUNDLED,
        )
    }

    private fun mergeCatalogs(
        bundledCatalog: GearCatalog,
        importedCatalog: GearCatalog,
    ): GearCatalog {
        return GearCatalog(
            cameraBodyProfiles = mergeById(bundledCatalog.cameraBodyProfiles, importedCatalog.cameraBodyProfiles) { profile -> profile.id },
            lensProfiles = mergeById(bundledCatalog.lensProfiles, importedCatalog.lensProfiles) { profile -> profile.id },
            source = GearCatalogSource.IMPORTED,
        )
    }

    private fun JSONArray.toCameraBodyProfiles(): List<CameraBodyProfile> {
        return List(length()) { index ->
            getJSONObject(index).toCameraBodyProfile()
        }
    }

    private fun JSONArray.toLensProfiles(): List<LensProfile> {
        return List(length()) { index ->
            getJSONObject(index).toLensProfile()
        }
    }

    private fun JSONObject.toCameraBodyProfile(): CameraBodyProfile {
        val nativeMount = LensMount.valueOf(getString("nativeMount"))
        return CameraBodyProfile(
            id = getString("id"),
            label = getString("label"),
            description = getString("description"),
            category = CameraBodyCategory.valueOf(getString("category")),
            cropFactor = optDouble("cropFactor", 1.0).toFloat(),
            nativeMount = nativeMount,
            nativeCompatibleMounts = optJSONArray("nativeCompatibleMounts")?.toLensMountSet().orEmpty().ifEmpty {
                setOf(nativeMount)
            },
            adaptedCompatibleMounts = optJSONArray("adaptedCompatibleMounts")?.toLensMountSet().orEmpty(),
            hasInBodyStabilization = optBoolean("hasInBodyStabilization", false),
            minIso = getInt("minIso"),
            maxIso = getInt("maxIso"),
            fastestShutterSeconds = getDouble("fastestShutterSeconds"),
            longestStandardShutterSeconds = getDouble("longestStandardShutterSeconds"),
            supportsBulb = optBoolean("supportsBulb", true),
            meteringWorkflowNote = getString("meteringWorkflowNote"),
        )
    }

    private fun JSONObject.toLensProfile(): LensProfile {
        return LensProfile(
            id = getString("id"),
            label = getString("label"),
            description = getString("description"),
            mount = LensMount.valueOf(getString("mount")),
            minFocalLengthMm = getInt("minFocalLengthMm"),
            maxFocalLengthMm = optInt("maxFocalLengthMm", getInt("minFocalLengthMm")),
            widestApertureAtWideEnd = getDouble("widestApertureAtWideEnd").toFloat(),
            widestApertureAtTeleEnd = optDouble("widestApertureAtTeleEnd", getDouble("widestApertureAtWideEnd")).toFloat(),
            narrowestAperture = getDouble("narrowestAperture").toFloat(),
            hasOpticalStabilization = optBoolean("hasOpticalStabilization", false),
        )
    }

    private fun JSONArray.toLensMountSet(): Set<LensMount> {
        return List(length()) { index -> LensMount.valueOf(getString(index)) }.toSet()
    }

    private fun importedCatalogFile(context: Context): File = File(context.filesDir, IMPORTED_FILE_NAME)

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