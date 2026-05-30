package com.janhorak.shutterdeck.metering.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ShootingAidTest {

    @Test
    fun availableStabilizationModes_reflectsGearCapabilitiesInOrder() {
        assertEquals(
            listOf(StabilizationMode.OFF),
            availableStabilizationModes(
                hasLensStabilization = false,
                hasBodyStabilization = false,
            ),
        )
        assertEquals(
            listOf(StabilizationMode.OFF, StabilizationMode.LENS_ONLY),
            availableStabilizationModes(
                hasLensStabilization = true,
                hasBodyStabilization = false,
            ),
        )
        assertEquals(
            listOf(StabilizationMode.OFF, StabilizationMode.BODY_ONLY),
            availableStabilizationModes(
                hasLensStabilization = false,
                hasBodyStabilization = true,
            ),
        )
        assertEquals(
            listOf(
                StabilizationMode.OFF,
                StabilizationMode.LENS_ONLY,
                StabilizationMode.BODY_ONLY,
                StabilizationMode.HYBRID,
            ),
            availableStabilizationModes(
                hasLensStabilization = true,
                hasBodyStabilization = true,
            ),
        )
    }

    @Test
    fun scenePreset_resolvesBestSupportedStabilizationMode() {
        assertEquals(
            StabilizationMode.BODY_ONLY,
            ScenePreset.NIGHT_STREET.resolveStabilizationMode(
                listOf(StabilizationMode.OFF, StabilizationMode.BODY_ONLY),
            ),
        )
    }

    @Test
    fun matchingScenePreset_returnsCustomWhenPresetWouldResolveToDifferentMode() {
        assertEquals(
            ScenePreset.CUSTOM,
            matchingScenePreset(
                subjectMotionProfile = SubjectMotionProfile.SPORTS,
                stabilizationMode = StabilizationMode.OFF,
                availableModes = availableStabilizationModes(
                    hasLensStabilization = true,
                    hasBodyStabilization = true,
                ),
            ),
        )
        assertEquals(
            ScenePreset.WILDLIFE,
            matchingScenePreset(
                subjectMotionProfile = SubjectMotionProfile.SPORTS,
                stabilizationMode = StabilizationMode.OFF,
                availableModes = listOf(StabilizationMode.OFF),
            ),
        )
    }

    @Test
    fun assessShootingAid_awaitsMeteredShutterWhenNoneIsAvailable() {
        val assessment = assessShootingAid(
            measuredShutterSeconds = null,
            recommendedMinimumShutterSeconds = 1.0 / 125.0,
            subjectMotionProfile = SubjectMotionProfile.PORTRAIT,
            stabilizationMode = StabilizationMode.BODY_ONLY,
        )

        assertEquals(ShootingAidAssessmentLevel.AWAITING_READING, assessment.level)
        assertEquals(
            "Awaiting a metered shutter so the shooting aid can compare it to your handheld and subject-motion limits.",
            assessment.message,
        )
    }

    @Test
    fun assessShootingAid_marksSupportedExposure() {
        withLocale(Locale.US) {
            val assessment = assessShootingAid(
                measuredShutterSeconds = 1.0 / 250.0,
                recommendedMinimumShutterSeconds = 1.0 / 125.0,
                subjectMotionProfile = SubjectMotionProfile.PORTRAIT,
                stabilizationMode = StabilizationMode.LENS_ONLY,
            )

            assertEquals(ShootingAidAssessmentLevel.SUPPORTED, assessment.level)
            assertEquals(
                "The metered shutter is fast enough for portrait shooting with lens is.",
                assessment.message,
            )
        }
    }

    @Test
    fun assessShootingAid_marksBorderlineExposureWithinOneStop() {
        withLocale(Locale.US) {
            val assessment = assessShootingAid(
                measuredShutterSeconds = 1.0 / 30.0,
                recommendedMinimumShutterSeconds = 1.0 / 60.0,
                subjectMotionProfile = SubjectMotionProfile.STILL,
                stabilizationMode = StabilizationMode.OFF,
            )

            assertEquals(ShootingAidAssessmentLevel.BORDERLINE, assessment.level)
            assertEquals(
                "The metered shutter is borderline. About one stop faster would give more margin for still subjects.",
                assessment.message,
            )
        }
    }

    @Test
    fun assessShootingAid_marksTooSlowExposureWhenMoreThanOneStopBehind() {
        val assessment = assessShootingAid(
            measuredShutterSeconds = 1.0 / 15.0,
            recommendedMinimumShutterSeconds = 1.0 / 60.0,
            subjectMotionProfile = SubjectMotionProfile.ACTION,
            stabilizationMode = StabilizationMode.HYBRID,
        )

        assertEquals(ShootingAidAssessmentLevel.TOO_SLOW, assessment.level)
        assertEquals(
            "The metered shutter is too slow for this setup. Raise ISO, open the aperture, add support, or reduce subject motion.",
            assessment.message,
        )
    }

    private fun withLocale(locale: Locale, block: () -> Unit) {
        val previous = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            block()
        } finally {
            Locale.setDefault(previous)
        }
    }
}
