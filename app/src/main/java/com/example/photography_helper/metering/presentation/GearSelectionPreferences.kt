package com.example.photography_helper.metering.presentation

import android.content.Context

internal data class SavedGearSelection(
    val bodyId: String,
    val lensId: String,
    val allowAdaptedLenses: Boolean,
    val meteringSourceName: String,
    val stopModeName: String,
    val selectedIso: Int,
    val selectedAperture: Float,
    val calibrationOffset: Float,
)

internal class GearSelectionPreferences(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): SavedGearSelection? {
        val bodyId = sharedPreferences.getString(KEY_BODY_ID, null) ?: return null
        val lensId = sharedPreferences.getString(KEY_LENS_ID, null) ?: lensProfiles.first().id
        val allowAdaptedLenses = sharedPreferences.getBoolean(KEY_ALLOW_ADAPTED_LENSES, false)
        val meteringSourceName = sharedPreferences.getString(KEY_METERING_SOURCE_NAME, MeteringSource.AMBIENT_SENSOR.name)
            ?: MeteringSource.AMBIENT_SENSOR.name
        val stopModeName = sharedPreferences.getString(KEY_STOP_MODE_NAME, ExposureStopMode.FULL.name)
            ?: ExposureStopMode.FULL.name
        val selectedIso = sharedPreferences.getInt(KEY_SELECTED_ISO, 100)
        val selectedAperture = sharedPreferences.getFloat(KEY_SELECTED_APERTURE, 2.8f)
        val calibrationOffset = sharedPreferences.getFloat(KEY_CALIBRATION_OFFSET, 0f)
        return SavedGearSelection(
            bodyId = bodyId,
            lensId = lensId,
            allowAdaptedLenses = allowAdaptedLenses,
            meteringSourceName = meteringSourceName,
            stopModeName = stopModeName,
            selectedIso = selectedIso,
            selectedAperture = selectedAperture,
            calibrationOffset = calibrationOffset,
        )
    }

    fun save(selection: SavedGearSelection) {
        sharedPreferences.edit()
            .putString(KEY_BODY_ID, selection.bodyId)
            .putString(KEY_LENS_ID, selection.lensId)
            .putBoolean(KEY_ALLOW_ADAPTED_LENSES, selection.allowAdaptedLenses)
            .putString(KEY_METERING_SOURCE_NAME, selection.meteringSourceName)
            .putString(KEY_STOP_MODE_NAME, selection.stopModeName)
            .putInt(KEY_SELECTED_ISO, selection.selectedIso)
            .putFloat(KEY_SELECTED_APERTURE, selection.selectedAperture)
            .putFloat(KEY_CALIBRATION_OFFSET, selection.calibrationOffset)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "gear_selection"
        const val KEY_BODY_ID = "body_id"
        const val KEY_LENS_ID = "lens_id"
        const val KEY_ALLOW_ADAPTED_LENSES = "allow_adapted_lenses"
        const val KEY_METERING_SOURCE_NAME = "metering_source_name"
        const val KEY_STOP_MODE_NAME = "stop_mode_name"
        const val KEY_SELECTED_ISO = "selected_iso"
        const val KEY_SELECTED_APERTURE = "selected_aperture"
        const val KEY_CALIBRATION_OFFSET = "calibration_offset"
    }
}