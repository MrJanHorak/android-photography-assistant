package com.janhorak.shutterdeck.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.janhorak.shutterdeck.core.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** App-wide settings backed by Preferences DataStore. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val preferencesFlow: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    val themeMode: Flow<ThemeMode> = preferencesFlow
        .map { preferences ->
            preferences[KEY_THEME_MODE]
                ?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    val favoriteRoutes: Flow<Set<String>> = preferencesFlow
        .map { preferences -> preferences[KEY_FAVORITE_ROUTES] ?: emptySet() }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[KEY_THEME_MODE] = mode.name }
    }

    suspend fun toggleFavoriteRoute(route: String) {
        require(route.isNotBlank()) { "Favorite route must not be blank." }

        dataStore.edit { preferences ->
            val updatedRoutes = (preferences[KEY_FAVORITE_ROUTES] ?: emptySet()).toMutableSet()
            if (!updatedRoutes.add(route)) {
                updatedRoutes.remove(route)
            }
            preferences[KEY_FAVORITE_ROUTES] = updatedRoutes
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_FAVORITE_ROUTES = stringSetPreferencesKey("favorite_routes")
    }
}
