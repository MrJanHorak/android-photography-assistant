package com.janhorak.shutterdeck.metering.domain

import java.util.Locale
import kotlin.math.log2

enum class SubjectMotionProfile(
    val label: String,
    val description: String,
    val minimumShutterSeconds: Double,
) {
    STILL(
        label = "Still",
        description = "Landscapes, architecture, and other mostly static subjects.",
        minimumShutterSeconds = 1.0 / 30.0,
    ),
    PORTRAIT(
        label = "Portrait",
        description = "Human micro-movement and natural hand or face motion.",
        minimumShutterSeconds = 1.0 / 125.0,
    ),
    WALKING(
        label = "Walking",
        description = "Casual movement, kids walking, or documentary motion.",
        minimumShutterSeconds = 1.0 / 250.0,
    ),
    ACTION(
        label = "Action",
        description = "General sports, active children, or movement you want reasonably crisp.",
        minimumShutterSeconds = 1.0 / 500.0,
    ),
    SPORTS(
        label = "Sports",
        description = "Fast motion where freezing the subject matters more than low ISO.",
        minimumShutterSeconds = 1.0 / 1000.0,
    ),
}

enum class StabilizationMode(
    val label: String,
    val description: String,
    val stopsBenefit: Float,
) {
    OFF(
        label = "Off",
        description = "No stabilization help; use the classic handheld rule of thumb.",
        stopsBenefit = 0f,
    ),
    LENS_ONLY(
        label = "Lens IS",
        description = "Use the lens optical stabilizer only.",
        stopsBenefit = 3f,
    ),
    BODY_ONLY(
        label = "IBIS",
        description = "Use in-body stabilization only.",
        stopsBenefit = 3.5f,
    ),
    HYBRID(
        label = "Lens + IBIS",
        description = "Use coordinated optical and in-body stabilization.",
        stopsBenefit = 5f,
    ),
}

enum class ScenePreset(
    val label: String,
    val description: String,
    val subjectMotionProfile: SubjectMotionProfile?,
    private val stabilizationPreferenceOrder: List<StabilizationMode>,
) {
    CUSTOM(
        label = "Custom",
        description = "Keep your manual subject-motion and stabilization choices.",
        subjectMotionProfile = null,
        stabilizationPreferenceOrder = emptyList(),
    ),
    TRIPOD(
        label = "Tripod",
        description = "Static scenes on support. Stabilization is disabled and motion assumptions stay conservative.",
        subjectMotionProfile = SubjectMotionProfile.STILL,
        stabilizationPreferenceOrder = listOf(StabilizationMode.OFF),
    ),
    NIGHT_STREET(
        label = "Night street",
        description = "Walking people, available light, and as much stabilization as the kit can offer.",
        subjectMotionProfile = SubjectMotionProfile.WALKING,
        stabilizationPreferenceOrder = listOf(
            StabilizationMode.HYBRID,
            StabilizationMode.BODY_ONLY,
            StabilizationMode.LENS_ONLY,
            StabilizationMode.OFF,
        ),
    ),
    WILDLIFE(
        label = "Wildlife",
        description = "Fast subject motion with long lenses. The preset keeps the shutter bias aggressive.",
        subjectMotionProfile = SubjectMotionProfile.SPORTS,
        stabilizationPreferenceOrder = listOf(
            StabilizationMode.HYBRID,
            StabilizationMode.LENS_ONLY,
            StabilizationMode.BODY_ONLY,
            StabilizationMode.OFF,
        ),
    ),
    INDOOR_EVENT(
        label = "Indoor event",
        description = "People movement in low light, with stabilization enabled whenever the gear supports it.",
        subjectMotionProfile = SubjectMotionProfile.PORTRAIT,
        stabilizationPreferenceOrder = listOf(
            StabilizationMode.HYBRID,
            StabilizationMode.BODY_ONLY,
            StabilizationMode.LENS_ONLY,
            StabilizationMode.OFF,
        ),
    );

    fun resolveStabilizationMode(availableModes: List<StabilizationMode>): StabilizationMode? {
        return stabilizationPreferenceOrder.firstOrNull { mode -> mode in availableModes }
            ?: availableModes.firstOrNull()
    }
}

enum class ShootingAidAssessmentLevel {
    AWAITING_READING,
    SUPPORTED,
    BORDERLINE,
    TOO_SLOW,
}

data class ShootingAidAssessment(
    val message: String,
    val level: ShootingAidAssessmentLevel,
)

fun availableStabilizationModes(
    hasLensStabilization: Boolean,
    hasBodyStabilization: Boolean,
): List<StabilizationMode> {
    return buildList {
        add(StabilizationMode.OFF)
        if (hasLensStabilization) {
            add(StabilizationMode.LENS_ONLY)
        }
        if (hasBodyStabilization) {
            add(StabilizationMode.BODY_ONLY)
        }
        if (hasLensStabilization && hasBodyStabilization) {
            add(StabilizationMode.HYBRID)
        }
    }
}

fun matchingScenePreset(
    subjectMotionProfile: SubjectMotionProfile,
    stabilizationMode: StabilizationMode,
    availableModes: List<StabilizationMode>,
): ScenePreset {
    return ScenePreset.entries.firstOrNull { preset ->
        preset != ScenePreset.CUSTOM &&
            preset.subjectMotionProfile == subjectMotionProfile &&
            preset.resolveStabilizationMode(availableModes) == stabilizationMode
    } ?: ScenePreset.CUSTOM
}

fun assessShootingAid(
    measuredShutterSeconds: Double?,
    recommendedMinimumShutterSeconds: Double,
    subjectMotionProfile: SubjectMotionProfile,
    stabilizationMode: StabilizationMode,
): ShootingAidAssessment {
    val measuredSeconds = measuredShutterSeconds ?: return ShootingAidAssessment(
        message = "Awaiting a metered shutter so the shooting aid can compare it to your handheld and subject-motion limits.",
        level = ShootingAidAssessmentLevel.AWAITING_READING,
    )
    val deltaStops = log2(measuredSeconds / recommendedMinimumShutterSeconds)

    return when {
        deltaStops <= 0.0 -> {
            ShootingAidAssessment(
                message = "The metered shutter is fast enough for ${subjectMotionProfile.label.lowercase(Locale.getDefault())} shooting with ${stabilizationMode.label.lowercase(Locale.getDefault())}.",
                level = ShootingAidAssessmentLevel.SUPPORTED,
            )
        }

        deltaStops <= 1.0 -> {
            ShootingAidAssessment(
                message = "The metered shutter is borderline. About one stop faster would give more margin for ${subjectMotionProfile.label.lowercase(Locale.getDefault())} subjects.",
                level = ShootingAidAssessmentLevel.BORDERLINE,
            )
        }

        else -> {
            ShootingAidAssessment(
                message = "The metered shutter is too slow for this setup. Raise ISO, open the aperture, add support, or reduce subject motion.",
                level = ShootingAidAssessmentLevel.TOO_SLOW,
            )
        }
    }
}
