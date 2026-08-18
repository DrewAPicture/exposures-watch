package com.exposures.watch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.watch.settings.OfflineModePreferences
import com.exposures.watch.sync.OfflineModeQueueFlusher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WatchSettingsUiState(
    val offlineModeEnabled: Boolean = false,
)

class WatchSettingsViewModel(
    private val offlineModePreferences: OfflineModePreferences,
    private val offlineModeQueueFlusher: OfflineModeQueueFlusher,
) : ViewModel() {

    val uiState: StateFlow<WatchSettingsUiState> = offlineModePreferences.enabled
        .map { enabled -> WatchSettingsUiState(offlineModeEnabled = enabled) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchSettingsUiState())

    fun setOfflineModeEnabled(enabled: Boolean) {
        offlineModePreferences.setEnabled(enabled)
        if (!enabled) {
            viewModelScope.launch {
                offlineModeQueueFlusher.flushAll()
            }
        }
    }
}
