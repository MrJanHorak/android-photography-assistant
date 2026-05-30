package com.janhorak.shutterdeck.metering.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MeterReadoutTest {

    @Test
    fun formatSuggestedShutter_waitsForSensorDataWithoutReading() {
        assertEquals(
            "Awaiting sensor data",
            formatSuggestedShutter(
                shutterSpeedSeconds = null,
                shutterOptions = shutterOptions(),
                supportsBulb = false,
            ),
        )
    }

    @Test
    fun formatSuggestedShutter_usesBulbWhenLongerThanCatalogAndSupported() {
        assertEquals(
            "Bulb ~ 45 s",
            formatSuggestedShutter(
                shutterSpeedSeconds = 45.0,
                shutterOptions = shutterOptions(),
                supportsBulb = true,
            ),
        )
    }

    @Test
    fun formatSuggestedShutter_reportsLongerThanLongestStandardWithoutBulb() {
        assertEquals(
            "Longer than 30 s",
            formatSuggestedShutter(
                shutterSpeedSeconds = 45.0,
                shutterOptions = shutterOptions(),
                supportsBulb = false,
            ),
        )
    }

    @Test
    fun formatSuggestedShutter_picksNearestStandardByStopDistance() {
        assertEquals(
            "1/60 s",
            formatSuggestedShutter(
                shutterSpeedSeconds = 1.0 / 80.0,
                shutterOptions = shutterOptions(),
                supportsBulb = false,
            ),
        )
    }

    @Test
    fun formatCameraExposureSummary_formatsReadableAeSummary() {
        assertEquals(
            "f/2.8  1/125 s  ISO 400",
            formatCameraExposureSummary(
                ReflectiveMeterReading(
                    aperture = 2.8f,
                    shutterSeconds = 1.0 / 125.0,
                    iso = 400,
                    ev100 = 10.6f,
                ),
            ),
        )
    }

    private fun shutterOptions(): List<ShutterOption> {
        return listOf(
            ShutterOption(30.0, "30 s"),
            ShutterOption(1.0, "1 s"),
            ShutterOption(1.0 / 60.0, "1/60 s"),
            ShutterOption(1.0 / 125.0, "1/125 s"),
        )
    }
}
