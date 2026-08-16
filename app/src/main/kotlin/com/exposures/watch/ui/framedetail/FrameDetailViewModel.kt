package com.exposures.watch.ui.framedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.Exposure
import com.exposures.model.Lens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FrameDetailUiState(
    val isLoading: Boolean = true,
    val exposure: Exposure? = null,
    val lens: Lens? = null,
)

class FrameDetailViewModel(
    private val repository: ExposureRepository,
    private val exposureId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrameDetailUiState())
    val uiState: StateFlow<FrameDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exposure = repository.getExposure(exposureId)
            val lens = exposure?.let { repository.getLens(it.lensId) }
            _uiState.value = FrameDetailUiState(isLoading = false, exposure = exposure, lens = lens)
        }
    }
}
