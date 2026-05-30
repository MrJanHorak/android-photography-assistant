package com.janhorak.shutterdeck.metering.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WorkflowCoachingTest {

    @Test
    fun workflowPriorityNames_matchPersistedPreferencesContract() {
        assertEquals("ISO_FIRST", WorkflowPriority.ISO_FIRST.name)
        assertEquals("APERTURE_FIRST", WorkflowPriority.APERTURE_FIRST.name)
        assertEquals("SHUTTER_FIRST", WorkflowPriority.SHUTTER_FIRST.name)
    }

    @Test
    fun buildExposureWorkflowSuggestion_awaitsMeteredShutter() {
        val suggestion = buildExposureWorkflowSuggestion(
            workflowPriority = WorkflowPriority.ISO_FIRST,
            measuredShutterSeconds = null,
            recommendedMinimumShutterSeconds = 1.0 / 60.0,
            currentAperture = 4.0f,
            currentIso = 100,
            apertureOptions = apertureOptions(),
            isoOptions = isoOptions(),
        )

        assertEquals(
            "Awaiting a metered shutter before workflow coaching can suggest what to change first.",
            suggestion,
        )
    }

    @Test
    fun buildExposureWorkflowSuggestion_treatsTargetBoundaryAsAlreadySafe() {
        withLocale(Locale.US) {
            val suggestion = buildExposureWorkflowSuggestion(
                workflowPriority = WorkflowPriority.ISO_FIRST,
                measuredShutterSeconds = 1.0 / 125.0,
                recommendedMinimumShutterSeconds = 1.0 / 125.0,
                currentAperture = 4.0f,
                currentIso = 100,
                apertureOptions = apertureOptions(),
                isoOptions = isoOptions(),
            )

            assertEquals(
                "Current settings already meet the practical shutter target. Use iso first only if you want more safety margin.",
                suggestion,
            )
        }
    }

    @Test
    fun buildExposureWorkflowSuggestion_isoFirstCanSolveWithIsoOnly() {
        withLocale(Locale.US) {
            val suggestion = buildExposureWorkflowSuggestion(
                workflowPriority = WorkflowPriority.ISO_FIRST,
                measuredShutterSeconds = 1.0 / 30.0,
                recommendedMinimumShutterSeconds = 1.0 / 60.0,
                currentAperture = 4.0f,
                currentIso = 100,
                apertureOptions = apertureOptions(),
                isoOptions = isoOptions(),
            )

            assertEquals(
                "Raise ISO first to ISO 200 and keep f/4. That should support 1/60 s.",
                suggestion,
            )
        }
    }

    @Test
    fun buildExposureWorkflowSuggestion_apertureFirstCanNeedApertureThenIso() {
        withLocale(Locale.US) {
            val suggestion = buildExposureWorkflowSuggestion(
                workflowPriority = WorkflowPriority.APERTURE_FIRST,
                measuredShutterSeconds = 1.0 / 10.0,
                recommendedMinimumShutterSeconds = 1.0 / 60.0,
                currentAperture = 5.6f,
                currentIso = 100,
                apertureOptions = listOf(
                    ExposureOption(2.8f, "f/2.8"),
                    ExposureOption(4.0f, "f/4"),
                    ExposureOption(5.6f, "f/5.6"),
                ),
                isoOptions = listOf(
                    ExposureOption(100, "ISO 100"),
                    ExposureOption(200, "ISO 200"),
                ),
            )

            assertEquals(
                "Open to f/2.8 first, then raise ISO to ISO 200 to support 1/60 s.",
                suggestion,
            )
        }
    }

    @Test
    fun buildExposureWorkflowSuggestion_shutterFirstCanStayShortAfterIsoFallback() {
        withLocale(Locale.US) {
            val suggestion = buildExposureWorkflowSuggestion(
                workflowPriority = WorkflowPriority.SHUTTER_FIRST,
                measuredShutterSeconds = 1.0 / 4.0,
                recommendedMinimumShutterSeconds = 1.0 / 60.0,
                currentAperture = 4.0f,
                currentIso = 100,
                apertureOptions = listOf(
                    ExposureOption(2.8f, "f/2.8"),
                    ExposureOption(4.0f, "f/4"),
                ),
                isoOptions = listOf(
                    ExposureOption(100, "ISO 100"),
                    ExposureOption(200, "ISO 200"),
                ),
            )

            assertEquals(
                "Dial 1/60 s first. Even at f/2.8 and ISO 200, you are still about 1.9 stops short.",
                suggestion,
            )
        }
    }

    @Test
    fun nearestExposureIndex_usesStopDistanceRatherThanLinearDistance() {
        val index = nearestExposureIndex(
            options = listOf(
                ExposureOption(2.0f, "f/2"),
                ExposureOption(4.0f, "f/4"),
            ),
            selectedValue = 3.0,
        )

        assertEquals(1, index)
    }

    private fun apertureOptions(): List<ExposureOption<Float>> {
        return listOf(
            ExposureOption(2.0f, "f/2"),
            ExposureOption(2.8f, "f/2.8"),
            ExposureOption(4.0f, "f/4"),
        )
    }

    private fun isoOptions(): List<ExposureOption<Int>> {
        return listOf(
            ExposureOption(100, "ISO 100"),
            ExposureOption(200, "ISO 200"),
            ExposureOption(400, "ISO 400"),
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
