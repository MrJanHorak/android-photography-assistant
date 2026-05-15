package com.example.photography_helper.metering.presentation

import android.content.Context

internal data class SavedGearSelection(
    val bodyId: String,
    val lensId: String,
    val allowAdaptedLenses: Boolean,
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
        return SavedGearSelection(
            bodyId = bodyId,
            lensId = lensId,
            allowAdaptedLenses = allowAdaptedLenses,
        )
    }

    fun save(selection: SavedGearSelection) {
        sharedPreferences.edit()
            .putString(KEY_BODY_ID, selection.bodyId)
            .putString(KEY_LENS_ID, selection.lensId)
            .putBoolean(KEY_ALLOW_ADAPTED_LENSES, selection.allowAdaptedLenses)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "gear_selection"
        const val KEY_BODY_ID = "body_id"
        const val KEY_LENS_ID = "lens_id"
        const val KEY_ALLOW_ADAPTED_LENSES = "allow_adapted_lenses"
    }
}