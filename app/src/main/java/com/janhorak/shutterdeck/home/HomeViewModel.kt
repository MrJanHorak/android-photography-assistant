package com.janhorak.shutterdeck.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val favoriteRoutes: StateFlow<Set<String>> = settingsRepository.favoriteRoutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    val recentRoutes: StateFlow<List<String>> = settingsRepository.recentRoutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun toggleFavorite(route: String) {
        viewModelScope.launch { settingsRepository.toggleFavoriteRoute(route) }
    }

    fun recordRecent(route: String) {
        viewModelScope.launch { settingsRepository.recordRecentRoute(route) }
    }
}
