package com.example.photography_helper.metering.presentation

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class GearCatalog(
    val cameraBodyProfiles: List<CameraBodyProfile>,
    val lensProfiles: List<LensProfile>,
)

internal object GearCatalogLoader {
    private const val ASSET_FILE_NAME = "gear_catalog.json"

    fun load(context: Context): GearCatalog {
        return runCatching {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                parse(reader.readText())
            }
        }.getOrElse {
            GearCatalog(
                cameraBodyProfiles = cameraBodyProfiles,
                lensProfiles = lensProfiles,
            )
        }
    }

    private fun parse(json: String): GearCatalog {
        val root = JSONObject(json)
        val parsedBodies = root.optJSONArray("cameraBodyProfiles")?.toCameraBodyProfiles().orEmpty()
        val parsedLenses = root.optJSONArray("lensProfiles")?.toLensProfiles().orEmpty()

        return GearCatalog(
            cameraBodyProfiles = parsedBodies.ifEmpty { cameraBodyProfiles },
            lensProfiles = parsedLenses.ifEmpty { lensProfiles },
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
}