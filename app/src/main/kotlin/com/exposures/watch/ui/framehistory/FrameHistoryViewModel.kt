package com.exposures.watch.ui.framehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.Exposure
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FrameHistoryUiState(
    val isLoading: Boolean = true,
    val exposures: List<Exposure> = emptyList(),
)

class FrameHistoryViewModel(repository: ExposureRepository, rollId: String) : ViewModel() {

    val uiState: StateFlow<FrameHistoryUiState> = repository.observeExposures(rollId)
        .map { FrameHistoryUiState(isLoading = false, exposures = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FrameHistoryUiState())
}
