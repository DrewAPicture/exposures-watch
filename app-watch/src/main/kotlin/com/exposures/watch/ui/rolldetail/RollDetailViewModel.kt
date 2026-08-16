package com.exposures.watch.ui.rolldetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmRoll
import com.exposures.model.isComplete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RollDetailUiState(
    val isLoading: Boolean = true,
    val roll: FilmRoll? = null,
    val cameraBody: CameraBody? = null,
    val exposureCount: Int = 0,
    val lastExposure: Exposure? = null,
    val isComplete: Boolean = false,
)

class RollDetailViewModel(
    private val repository: ExposureRepository,
    private val rollId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RollDetailUiState())
    val uiState: StateFlow<RollDetailUiState> = _uiState.asStateFlow()

    private var loadedCameraBodyId: String? = null

    init {
        viewModelScope.launch {
            combine(
                repository.observeRoll(rollId),
                repository.observeExposures(rollId),
            ) { roll, exposures -> roll to exposures }.collect { (roll, exposures) ->
                val cameraBody = when {
                    roll == null -> null
                    roll.cameraBodyId == loadedCameraBodyId -> _uiState.value.cameraBody
                    else -> repository.getCameraBody(roll.cameraBodyId).also { loadedCameraBodyId = roll.cameraBodyId }
                }
                _uiState.value = RollDetailUiState(
                    isLoading = false,
                    roll = roll,
                    cameraBody = cameraBody,
                    exposureCount = exposures.size,
                    lastExposure = exposures.maxByOrNull { it.frameNumber },
                    isComplete = roll?.isComplete(exposures.size) ?: false,
                )
            }
        }
    }
}
