package com.example.photography_helper.metering.presentation

import kotlin.math.abs

internal data class ExposureOption<T : Number>(
    val value: T,
    val label: String,
)

internal data class ShutterOption(
    val seconds: Double,
    val label: String,
)

internal enum class LensMount(val label: String) {
    GENERIC("Any mount"),
    CANON_RF("Canon RF"),
    CANON_EF("Canon EF"),
    CANON_FD("Canon FD"),
    NIKON_Z("Nikon Z"),
    NIKON_F("Nikon F"),
    PENTAX_K("Pentax K"),
    SONY_FE("Sony FE"),
    SONY_E("Sony E"),
}

internal enum class CameraBodyCategory(val label: String) {
    DIGITAL("Digital"),
    MANUAL_FILM("Manual / film"),
}

internal enum class ExposureStopMode(
    val label: String,
    val apertureDescription: String,
    val isoDescription: String,
    val apertureOptions: List<ExposureOption<Float>>,
    val isoOptions: List<ExposureOption<Int>>,
    val shutterOptions: List<ShutterOption>,
) {
    FULL(
        label = "Full stop",
        apertureDescription = "Common full-stop settings for quick photographic adjustments.",
        isoDescription = "Common full-stop ISO values used by many cameras.",
        apertureOptions = FULL_STOP_APERTURES,
        isoOptions = FULL_STOP_ISOS,
        shutterOptions = FULL_STOP_SHUTTERS,
    ),
    THIRD(
        label = "1/3 stop",
        apertureDescription = "One-third-stop settings that better match most modern camera dials.",
        isoDescription = "One-third-stop ISO values commonly exposed in digital camera menus.",
        apertureOptions = THIRD_STOP_APERTURES,
        isoOptions = THIRD_STOP_ISOS,
        shutterOptions = THIRD_STOP_SHUTTERS,
    )
}

internal data class CameraBodyProfile(
    val id: String,
    val label: String,
    val description: String,
    val category: CameraBodyCategory,
    val cropFactor: Float = 1.0f,
    val nativeMount: LensMount,
    val nativeCompatibleMounts: Set<LensMount> = setOf(nativeMount),
    val adaptedCompatibleMounts: Set<LensMount> = emptySet(),
    val minIso: Int,
    val maxIso: Int,
    val fastestShutterSeconds: Double,
    val longestStandardShutterSeconds: Double,
    val supportsBulb: Boolean = true,
    val meteringWorkflowNote: String,
) {
    val nativeCompatibilitySummary: String
        get() = when {
            nativeMount == LensMount.GENERIC -> LensMount.GENERIC.label
            nativeCompatibleMounts == setOf(nativeMount) -> nativeMount.label
            else -> nativeCompatibleMounts
                .sortedBy { mount -> mount.label }
                .joinToString { mount -> mount.label }
        }

    val adaptedCompatibilitySummary: String
        get() = adaptedCompatibleMounts
            .sortedBy { mount -> mount.label }
            .joinToString { mount -> mount.label }

    fun availableMounts(allowAdaptedLenses: Boolean): Set<LensMount> {
        return buildSet {
            addAll(nativeCompatibleMounts)
            if (allowAdaptedLenses) {
                addAll(adaptedCompatibleMounts)
            }
        }
    }

    fun filterIsos(options: List<ExposureOption<Int>>): List<ExposureOption<Int>> {
        return options.filter { option -> option.value in minIso..maxIso }
            .ifEmpty {
                listOf(options.minByOrNull { option -> abs(option.value - minIso) } ?: options.first())
            }
    }

    fun filterShutters(options: List<ShutterOption>): List<ShutterOption> {
        return options.filter { option -> option.seconds in fastestShutterSeconds..longestStandardShutterSeconds }
            .ifEmpty { options }
    }

    fun supportsLens(lens: LensProfile, allowAdaptedLenses: Boolean): Boolean {
        return nativeMount == LensMount.GENERIC ||
            lens.mount == LensMount.GENERIC ||
            lens.mount in availableMounts(allowAdaptedLenses)
    }
}

internal data class LensProfile(
    val id: String,
    val label: String,
    val description: String,
    val mount: LensMount,
    val minFocalLengthMm: Int,
    val maxFocalLengthMm: Int = minFocalLengthMm,
    val widestApertureAtWideEnd: Float,
    val widestApertureAtTeleEnd: Float = widestApertureAtWideEnd,
    val narrowestAperture: Float,
) {
    val isZoom: Boolean
        get() = minFocalLengthMm != maxFocalLengthMm

    val focalLengthRangeLabel: String
        get() = if (isZoom) {
            "${minFocalLengthMm}-${maxFocalLengthMm}mm"
        } else {
            "${minFocalLengthMm}mm"
        }

    val widestApertureRangeLabel: String
        get() = if (abs(widestApertureAtWideEnd - widestApertureAtTeleEnd) < 0.05f) {
            "f/${formatCatalogDecimal(widestApertureAtWideEnd.toDouble())}"
        } else {
            "f/${formatCatalogDecimal(widestApertureAtWideEnd.toDouble())}-${formatCatalogDecimal(widestApertureAtTeleEnd.toDouble())}"
        }

    val mountSummary: String
        get() = if (mount == LensMount.GENERIC) {
            "Works with any body profile"
        } else {
            "Mount: ${mount.label}"
        }

    fun defaultFocalLengthMm(): Int = minFocalLengthMm

    fun clampFocalLength(focalLengthMm: Int): Int = focalLengthMm.coerceIn(minFocalLengthMm, maxFocalLengthMm)

    fun focalLengthLabel(focalLengthMm: Int): String = "${clampFocalLength(focalLengthMm)}mm"

    fun effectiveWidestAperture(focalLengthMm: Int): Float {
        val clampedFocalLength = clampFocalLength(focalLengthMm)
        if (!isZoom || abs(widestApertureAtWideEnd - widestApertureAtTeleEnd) < 0.05f) {
            return widestApertureAtWideEnd
        }

        val ratio = (clampedFocalLength - minFocalLengthMm).toFloat() / (maxFocalLengthMm - minFocalLengthMm).toFloat()
        return widestApertureAtWideEnd + ((widestApertureAtTeleEnd - widestApertureAtWideEnd) * ratio)
    }

    fun filterApertures(
        options: List<ExposureOption<Float>>,
        focalLengthMm: Int,
    ): List<ExposureOption<Float>> {
        val widestUsableAperture = effectiveWidestAperture(focalLengthMm)
        return options.filter { option -> option.value in widestUsableAperture..narrowestAperture }
            .ifEmpty {
                listOf(options.minByOrNull { option -> abs(option.value - widestUsableAperture) } ?: options.first())
            }
    }
}

internal fun compatibleLensProfiles(
    body: CameraBodyProfile,
    allowAdaptedLenses: Boolean,
): List<LensProfile> {
    return lensProfiles.filter { lens -> body.supportsLens(lens, allowAdaptedLenses) }
        .ifEmpty {
            lensProfiles.filter { lens -> lens.mount == LensMount.GENERIC }
                .ifEmpty { lensProfiles.take(1) }
        }
}

internal val cameraBodyProfiles = listOf(
    CameraBodyProfile(
        id = "generic_digital",
        label = "Generic digital body",
        description = "Fallback preset with a common 30 s to 1/8000 s shutter range and ISO 100 to 6400.",
        category = CameraBodyCategory.DIGITAL,
        cropFactor = 1.0f,
        nativeMount = LensMount.GENERIC,
        nativeCompatibleMounts = setOf(LensMount.GENERIC),
        minIso = 100,
        maxIso = 6400,
        fastestShutterSeconds = 1.0 / 8000.0,
        longestStandardShutterSeconds = 30.0,
        meteringWorkflowNote = "Treat ISO as the camera sensitivity setting you plan to dial in.",
    ),
    CameraBodyProfile(
        id = "canon_eos_r6_mk2",
        label = "Canon EOS R6 Mark II",
        description = "Full-frame mirrorless preset with ISO 100 to 102400 and a standard 30 s to 1/8000 s shutter range.",
        category = CameraBodyCategory.DIGITAL,
        cropFactor = 1.0f,
        nativeMount = LensMount.CANON_RF,
        nativeCompatibleMounts = setOf(LensMount.CANON_RF),
        adaptedCompatibleMounts = setOf(LensMount.CANON_EF),
        minIso = 100,
        maxIso = 102400,
        fastestShutterSeconds = 1.0 / 8000.0,
        longestStandardShutterSeconds = 30.0,
        meteringWorkflowNote = "Use the selected ISO as the digital camera sensitivity target.",
    ),
    CameraBodyProfile(
        id = "canon_eos_r10",
        label = "Canon EOS R10",
        description = "APS-C mirrorless preset with ISO 100 to 32000 and a conservative 30 s to 1/4000 s shutter range.",
        category = CameraBodyCategory.DIGITAL,
        cropFactor = 1.6f,
        nativeMount = LensMount.CANON_RF,
        nativeCompatibleMounts = setOf(LensMount.CANON_RF),
        adaptedCompatibleMounts = setOf(LensMount.CANON_EF),
        minIso = 100,
        maxIso = 32000,
        fastestShutterSeconds = 1.0 / 4000.0,
        longestStandardShutterSeconds = 30.0,
        meteringWorkflowNote = "Use the selected ISO as the digital camera sensitivity target.",
    ),
    CameraBodyProfile(
        id = "sony_a7_iv",
        label = "Sony a7 IV",
        description = "Full-frame mirrorless preset with ISO 100 to 51200 and a standard 30 s to 1/8000 s shutter range.",
        category = CameraBodyCategory.DIGITAL,
        cropFactor = 1.0f,
        nativeMount = LensMount.SONY_FE,
        nativeCompatibleMounts = setOf(LensMount.SONY_FE, LensMount.SONY_E),
        minIso = 100,
        maxIso = 51200,
        fastestShutterSeconds = 1.0 / 8000.0,
        longestStandardShutterSeconds = 30.0,
        meteringWorkflowNote = "Use the selected ISO as the digital camera sensitivity target.",
    ),
    CameraBodyProfile(
        id = "nikon_z6_ii",
        label = "Nikon Z6 II",
        description = "Full-frame mirrorless preset with ISO 100 to 51200 and a standard 30 s to 1/8000 s shutter range.",
        category = CameraBodyCategory.DIGITAL,
        cropFactor = 1.0f,
        nativeMount = LensMount.NIKON_Z,
        nativeCompatibleMounts = setOf(LensMount.NIKON_Z),
        adaptedCompatibleMounts = setOf(LensMount.NIKON_F),
        minIso = 100,
        maxIso = 51200,
        fastestShutterSeconds = 1.0 / 8000.0,
        longestStandardShutterSeconds = 30.0,
        meteringWorkflowNote = "Use the selected ISO as the digital camera sensitivity target.",
    ),
    CameraBodyProfile(
        id = "nikon_fm2",
        label = "Nikon FM2",
        description = "Manual 35mm film SLR preset with a mechanical 1 s to 1/4000 s shutter range plus bulb.",
        category = CameraBodyCategory.MANUAL_FILM,
        cropFactor = 1.0f,
        nativeMount = LensMount.NIKON_F,
        nativeCompatibleMounts = setOf(LensMount.NIKON_F),
        minIso = 25,
        maxIso = 3200,
        fastestShutterSeconds = 1.0 / 4000.0,
        longestStandardShutterSeconds = 1.0,
        supportsBulb = true,
        meteringWorkflowNote = "Treat ISO as the loaded film speed rather than an in-camera gain setting.",
    ),
    CameraBodyProfile(
        id = "canon_ae_1_program",
        label = "Canon AE-1 Program",
        description = "Manual-focus film SLR preset with a 2 s to 1/1000 s shutter range plus bulb.",
        category = CameraBodyCategory.MANUAL_FILM,
        cropFactor = 1.0f,
        nativeMount = LensMount.CANON_FD,
        nativeCompatibleMounts = setOf(LensMount.CANON_FD),
        minIso = 25,
        maxIso = 3200,
        fastestShutterSeconds = 1.0 / 1000.0,
        longestStandardShutterSeconds = 2.0,
        supportsBulb = true,
        meteringWorkflowNote = "Treat ISO as the loaded film speed rather than an in-camera gain setting.",
    ),
    CameraBodyProfile(
        id = "pentax_k1000",
        label = "Pentax K1000",
        description = "Classic manual 35mm film SLR preset with a 1 s to 1/1000 s shutter range plus bulb.",
        category = CameraBodyCategory.MANUAL_FILM,
        cropFactor = 1.0f,
        nativeMount = LensMount.PENTAX_K,
        nativeCompatibleMounts = setOf(LensMount.PENTAX_K),
        minIso = 20,
        maxIso = 3200,
        fastestShutterSeconds = 1.0 / 1000.0,
        longestStandardShutterSeconds = 1.0,
        supportsBulb = true,
        meteringWorkflowNote = "Treat ISO as the loaded film speed rather than an in-camera gain setting.",
    ),
)

internal val lensProfiles = listOf(
    LensProfile(
        id = "generic_24_70_28",
        label = "Generic 24-70mm f/2.8",
        description = "Flexible fallback preset when the exact lens family is not important yet.",
        mount = LensMount.GENERIC,
        minFocalLengthMm = 24,
        maxFocalLengthMm = 70,
        widestApertureAtWideEnd = 2.8f,
        widestApertureAtTeleEnd = 2.8f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "canon_rf_24_70_28",
        label = "Canon RF 24-70mm F2.8L IS USM",
        description = "Constant-aperture professional zoom preset.",
        mount = LensMount.CANON_RF,
        minFocalLengthMm = 24,
        maxFocalLengthMm = 70,
        widestApertureAtWideEnd = 2.8f,
        widestApertureAtTeleEnd = 2.8f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "canon_rf_24_105_4",
        label = "Canon RF 24-105mm F4L IS USM",
        description = "Constant-aperture travel zoom preset.",
        mount = LensMount.CANON_RF,
        minFocalLengthMm = 24,
        maxFocalLengthMm = 105,
        widestApertureAtWideEnd = 4.0f,
        widestApertureAtTeleEnd = 4.0f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "canon_rf_24_240_4_63",
        label = "Canon RF 24-240mm F4-6.3 IS USM",
        description = "Travel zoom preset with a variable maximum aperture across the zoom range.",
        mount = LensMount.CANON_RF,
        minFocalLengthMm = 24,
        maxFocalLengthMm = 240,
        widestApertureAtWideEnd = 4.0f,
        widestApertureAtTeleEnd = 6.3f,
        narrowestAperture = 40.0f,
    ),
    LensProfile(
        id = "canon_ef_50_14",
        label = "Canon EF 50mm f/1.4 USM",
        description = "Adapted fast normal prime preset with f/1.4 to f/22 coverage.",
        mount = LensMount.CANON_EF,
        minFocalLengthMm = 50,
        widestApertureAtWideEnd = 1.4f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "nikon_z_24_70_4",
        label = "Nikon Z 24-70mm f/4 S",
        description = "Constant-aperture standard zoom preset.",
        mount = LensMount.NIKON_Z,
        minFocalLengthMm = 24,
        maxFocalLengthMm = 70,
        widestApertureAtWideEnd = 4.0f,
        widestApertureAtTeleEnd = 4.0f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "nikon_z_24_200_4_63",
        label = "Nikon Z 24-200mm f/4-6.3 VR",
        description = "Travel zoom preset with a variable maximum aperture across the zoom range.",
        mount = LensMount.NIKON_Z,
        minFocalLengthMm = 24,
        maxFocalLengthMm = 200,
        widestApertureAtWideEnd = 4.0f,
        widestApertureAtTeleEnd = 6.3f,
        narrowestAperture = 32.0f,
    ),
    LensProfile(
        id = "nikon_f_50_18g",
        label = "Nikon AF-S 50mm f/1.8G",
        description = "Adapted fast prime preset with f/1.8 to f/16 coverage.",
        mount = LensMount.NIKON_F,
        minFocalLengthMm = 50,
        widestApertureAtWideEnd = 1.8f,
        narrowestAperture = 16.0f,
    ),
    LensProfile(
        id = "nikon_ai_s_50_18",
        label = "Nikon AI-S 50mm f/1.8",
        description = "Manual-focus normal prime preset for film and manual Nikon bodies.",
        mount = LensMount.NIKON_F,
        minFocalLengthMm = 50,
        widestApertureAtWideEnd = 1.8f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "canon_fd_50_14",
        label = "Canon FD 50mm f/1.4",
        description = "Manual-focus normal prime preset for Canon FD film bodies.",
        mount = LensMount.CANON_FD,
        minFocalLengthMm = 50,
        widestApertureAtWideEnd = 1.4f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "pentax_k_50_17",
        label = "SMC Pentax-M 50mm f/1.7",
        description = "Manual-focus normal prime preset for Pentax K film bodies.",
        mount = LensMount.PENTAX_K,
        minFocalLengthMm = 50,
        widestApertureAtWideEnd = 1.7f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "sony_fe_24_70_28_gm2",
        label = "Sony FE 24-70mm F2.8 GM II",
        description = "Constant-aperture professional zoom preset.",
        mount = LensMount.SONY_FE,
        minFocalLengthMm = 24,
        maxFocalLengthMm = 70,
        widestApertureAtWideEnd = 2.8f,
        widestApertureAtTeleEnd = 2.8f,
        narrowestAperture = 22.0f,
    ),
    LensProfile(
        id = "sony_fe_24_240_35_63",
        label = "Sony FE 24-240mm F3.5-6.3 OSS",
        description = "Travel zoom preset with a variable maximum aperture across the zoom range.",
        mount = LensMount.SONY_FE,
        minFocalLengthMm = 24,
        maxFocalLengthMm = 240,
        widestApertureAtWideEnd = 3.5f,
        widestApertureAtTeleEnd = 6.3f,
        narrowestAperture = 40.0f,
    ),
    LensProfile(
        id = "sony_fe_35_18",
        label = "Sony FE 35mm F1.8",
        description = "Compact fast prime preset with f/1.8 to f/22 coverage.",
        mount = LensMount.SONY_FE,
        minFocalLengthMm = 35,
        widestApertureAtWideEnd = 1.8f,
        narrowestAperture = 22.0f,
    ),
)

private fun formatCatalogDecimal(value: Double): String {
    return if (abs(value - value.toInt().toDouble()) < 0.05) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", value)
    }
}

private val FULL_STOP_APERTURES = listOf(
    ExposureOption(1.4f, "f/1.4"),
    ExposureOption(2.0f, "f/2"),
    ExposureOption(2.8f, "f/2.8"),
    ExposureOption(4.0f, "f/4"),
    ExposureOption(5.6f, "f/5.6"),
    ExposureOption(8.0f, "f/8"),
    ExposureOption(11.0f, "f/11"),
    ExposureOption(16.0f, "f/16"),
    ExposureOption(22.0f, "f/22"),
)

private val THIRD_STOP_APERTURES = listOf(
    ExposureOption(1.4f, "f/1.4"),
    ExposureOption(1.6f, "f/1.6"),
    ExposureOption(1.8f, "f/1.8"),
    ExposureOption(2.0f, "f/2"),
    ExposureOption(2.2f, "f/2.2"),
    ExposureOption(2.5f, "f/2.5"),
    ExposureOption(2.8f, "f/2.8"),
    ExposureOption(3.2f, "f/3.2"),
    ExposureOption(3.5f, "f/3.5"),
    ExposureOption(4.0f, "f/4"),
    ExposureOption(4.5f, "f/4.5"),
    ExposureOption(5.0f, "f/5"),
    ExposureOption(5.6f, "f/5.6"),
    ExposureOption(6.3f, "f/6.3"),
    ExposureOption(7.1f, "f/7.1"),
    ExposureOption(8.0f, "f/8"),
    ExposureOption(9.0f, "f/9"),
    ExposureOption(10.0f, "f/10"),
    ExposureOption(11.0f, "f/11"),
    ExposureOption(13.0f, "f/13"),
    ExposureOption(14.0f, "f/14"),
    ExposureOption(16.0f, "f/16"),
    ExposureOption(18.0f, "f/18"),
    ExposureOption(20.0f, "f/20"),
    ExposureOption(22.0f, "f/22"),
)

private val FULL_STOP_ISOS = listOf(
    ExposureOption(50, "ISO 50"),
    ExposureOption(100, "ISO 100"),
    ExposureOption(200, "ISO 200"),
    ExposureOption(400, "ISO 400"),
    ExposureOption(800, "ISO 800"),
    ExposureOption(1600, "ISO 1600"),
    ExposureOption(3200, "ISO 3200"),
    ExposureOption(6400, "ISO 6400"),
    ExposureOption(12800, "ISO 12800"),
    ExposureOption(25600, "ISO 25600"),
    ExposureOption(51200, "ISO 51200"),
    ExposureOption(102400, "ISO 102400"),
)

private val THIRD_STOP_ISOS = listOf(
    ExposureOption(50, "ISO 50"),
    ExposureOption(64, "ISO 64"),
    ExposureOption(80, "ISO 80"),
    ExposureOption(100, "ISO 100"),
    ExposureOption(125, "ISO 125"),
    ExposureOption(160, "ISO 160"),
    ExposureOption(200, "ISO 200"),
    ExposureOption(250, "ISO 250"),
    ExposureOption(320, "ISO 320"),
    ExposureOption(400, "ISO 400"),
    ExposureOption(500, "ISO 500"),
    ExposureOption(640, "ISO 640"),
    ExposureOption(800, "ISO 800"),
    ExposureOption(1000, "ISO 1000"),
    ExposureOption(1250, "ISO 1250"),
    ExposureOption(1600, "ISO 1600"),
    ExposureOption(2000, "ISO 2000"),
    ExposureOption(2500, "ISO 2500"),
    ExposureOption(3200, "ISO 3200"),
    ExposureOption(4000, "ISO 4000"),
    ExposureOption(5000, "ISO 5000"),
    ExposureOption(6400, "ISO 6400"),
    ExposureOption(8000, "ISO 8000"),
    ExposureOption(10000, "ISO 10000"),
    ExposureOption(12800, "ISO 12800"),
    ExposureOption(16000, "ISO 16000"),
    ExposureOption(20000, "ISO 20000"),
    ExposureOption(25600, "ISO 25600"),
    ExposureOption(32000, "ISO 32000"),
    ExposureOption(40000, "ISO 40000"),
    ExposureOption(51200, "ISO 51200"),
    ExposureOption(64000, "ISO 64000"),
    ExposureOption(80000, "ISO 80000"),
    ExposureOption(102400, "ISO 102400"),
)

private val FULL_STOP_SHUTTERS = listOf(
    ShutterOption(30.0, "30 s"),
    ShutterOption(15.0, "15 s"),
    ShutterOption(8.0, "8 s"),
    ShutterOption(4.0, "4 s"),
    ShutterOption(2.0, "2 s"),
    ShutterOption(1.0, "1 s"),
    ShutterOption(0.5, "1/2 s"),
    ShutterOption(0.25, "1/4 s"),
    ShutterOption(0.125, "1/8 s"),
    ShutterOption(1.0 / 15.0, "1/15 s"),
    ShutterOption(1.0 / 30.0, "1/30 s"),
    ShutterOption(1.0 / 60.0, "1/60 s"),
    ShutterOption(1.0 / 125.0, "1/125 s"),
    ShutterOption(1.0 / 250.0, "1/250 s"),
    ShutterOption(1.0 / 500.0, "1/500 s"),
    ShutterOption(1.0 / 1000.0, "1/1000 s"),
    ShutterOption(1.0 / 2000.0, "1/2000 s"),
    ShutterOption(1.0 / 4000.0, "1/4000 s"),
    ShutterOption(1.0 / 8000.0, "1/8000 s"),
)

private val THIRD_STOP_SHUTTERS = listOf(
    ShutterOption(30.0, "30 s"),
    ShutterOption(25.0, "25 s"),
    ShutterOption(20.0, "20 s"),
    ShutterOption(15.0, "15 s"),
    ShutterOption(13.0, "13 s"),
    ShutterOption(10.0, "10 s"),
    ShutterOption(8.0, "8 s"),
    ShutterOption(6.0, "6 s"),
    ShutterOption(5.0, "5 s"),
    ShutterOption(4.0, "4 s"),
    ShutterOption(3.2, "3.2 s"),
    ShutterOption(2.5, "2.5 s"),
    ShutterOption(2.0, "2 s"),
    ShutterOption(1.6, "1.6 s"),
    ShutterOption(1.3, "1.3 s"),
    ShutterOption(1.0, "1 s"),
    ShutterOption(0.8, "0.8 s"),
    ShutterOption(0.6, "0.6 s"),
    ShutterOption(0.5, "1/2 s"),
    ShutterOption(0.4, "0.4 s"),
    ShutterOption(1.0 / 3.0, "1/3 s"),
    ShutterOption(0.25, "1/4 s"),
    ShutterOption(0.2, "1/5 s"),
    ShutterOption(1.0 / 6.0, "1/6 s"),
    ShutterOption(0.125, "1/8 s"),
    ShutterOption(0.1, "1/10 s"),
    ShutterOption(1.0 / 13.0, "1/13 s"),
    ShutterOption(1.0 / 15.0, "1/15 s"),
    ShutterOption(0.05, "1/20 s"),
    ShutterOption(0.04, "1/25 s"),
    ShutterOption(1.0 / 30.0, "1/30 s"),
    ShutterOption(0.025, "1/40 s"),
    ShutterOption(0.02, "1/50 s"),
    ShutterOption(1.0 / 60.0, "1/60 s"),
    ShutterOption(1.0 / 80.0, "1/80 s"),
    ShutterOption(0.01, "1/100 s"),
    ShutterOption(1.0 / 125.0, "1/125 s"),
    ShutterOption(1.0 / 160.0, "1/160 s"),
    ShutterOption(0.005, "1/200 s"),
    ShutterOption(1.0 / 250.0, "1/250 s"),
    ShutterOption(1.0 / 320.0, "1/320 s"),
    ShutterOption(0.0025, "1/400 s"),
    ShutterOption(1.0 / 500.0, "1/500 s"),
    ShutterOption(1.0 / 640.0, "1/640 s"),
    ShutterOption(0.00125, "1/800 s"),
    ShutterOption(1.0 / 1000.0, "1/1000 s"),
    ShutterOption(1.0 / 1250.0, "1/1250 s"),
    ShutterOption(1.0 / 1600.0, "1/1600 s"),
    ShutterOption(1.0 / 2000.0, "1/2000 s"),
    ShutterOption(1.0 / 2500.0, "1/2500 s"),
    ShutterOption(1.0 / 3200.0, "1/3200 s"),
    ShutterOption(1.0 / 4000.0, "1/4000 s"),
    ShutterOption(1.0 / 5000.0, "1/5000 s"),
    ShutterOption(1.0 / 6400.0, "1/6400 s"),
    ShutterOption(1.0 / 8000.0, "1/8000 s"),
)