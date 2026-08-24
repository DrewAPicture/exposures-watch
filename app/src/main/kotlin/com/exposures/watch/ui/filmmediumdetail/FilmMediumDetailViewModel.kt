package com.exposures.watch.ui.filmmediumdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmMedium
import com.exposures.model.isComplete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FilmMediumDetailUiState(
    val isLoading: Boolean = true,
    val filmMedium: FilmMedium? = null,
    val cameraBody: CameraBody? = null,
    val exposureCount: Int = 0,
    val lastExposure: Exposure? = null,
    val isComplete: Boolean = false,
)

class FilmMediumDetailViewModel(
    private val repository: ExposureRepository,
    private val filmMediumId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilmMediumDetailUiState())
    val uiState: StateFlow<FilmMediumDetailUiState> = _uiState.asStateFlow()

    private var loadedCameraBodyId: String? = null

    init {
        viewModelScope.launch {
            combine(
                repository.observeFilmMedium(filmMediumId),
                repository.observeExposures(filmMediumId),
            ) { filmMedium, exposures -> filmMedium to exposures }.collect { (filmMedium, exposures) ->
                val cameraBody = when {
                    filmMedium == null -> null
                    filmMedium.cameraBodyId == loadedCameraBodyId -> _uiState.value.cameraBody
                    else -> repository.getCameraBody(filmMedium.cameraBodyId).also { loadedCameraBodyId = filmMedium.cameraBodyId }
                }
                _uiState.value = FilmMediumDetailUiState(
                    isLoading = false,
                    filmMedium = filmMedium,
                    cameraBody = cameraBody,
                    exposureCount = exposures.size,
                    lastExposure = exposures.maxByOrNull { it.frameNumber },
                    isComplete = filmMedium?.isComplete(exposures.size) ?: false,
                )
            }
        }
    }
}
