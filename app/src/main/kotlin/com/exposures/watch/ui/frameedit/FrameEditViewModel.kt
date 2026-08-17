package com.exposures.watch.ui.frameedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.Exposure
import com.exposures.model.Lens
import com.exposures.model.LightMeterType
import com.exposures.model.ShutterSpeed
import com.exposures.watch.sync.ExposurePusher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FrameEditUiState(
    val isLoading: Boolean = true,
    val lenses: List<Lens> = emptyList(),
    val availableShutterSpeeds: List<ShutterSpeed> = emptyList(),
    val availableApertures: List<Double> = emptyList(),
    val showZonePicker: Boolean = false,
    /** The exposure as currently edited — null only before load or if the exposure doesn't exist. */
    val draft: Exposure? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = draft != null && (!showZonePicker || draft.zone != null)
}

class FrameEditViewModel(
    private val repository: ExposureRepository,
    private val exposurePusher: ExposurePusher,
    private val exposureId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrameEditUiState())
    val uiState: StateFlow<FrameEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exposure = repository.getExposure(exposureId)
            val roll = exposure?.let { repository.getRoll(it.filmRollId) }
            val cameraBody = roll?.let { repository.getCameraBody(it.cameraBodyId) }
            val lenses = repository.observeLenses().first()
                .filter { lens -> lens.cameraBodyId == null || lens.cameraBodyId == roll?.cameraBodyId }
            val lightMeter = roll?.lightMeterId?.let { repository.getLightMeter(it) }
            val selectedLens = exposure?.let { e -> lenses.find { it.id == e.lensId } }

            _uiState.value = FrameEditUiState(
                isLoading = false,
                lenses = lenses,
                availableShutterSpeeds = cameraBody?.availableShutterSpeeds.orEmpty(),
                availableApertures = selectedLens?.availableApertures().orEmpty(),
                showZonePicker = lightMeter?.type == LightMeterType.SPOT,
                draft = exposure,
            )
        }
    }

    fun selectLens(lensId: String) {
        val state = _uiState.value
        val draft = state.draft ?: return
        val lens = state.lenses.find { it.id == lensId } ?: return
        val availableApertures = lens.availableApertures()
        _uiState.value = state.copy(
            draft = draft.copy(lensId = lensId, aperture = availableApertures.firstOrNull() ?: draft.aperture),
            availableApertures = availableApertures,
        )
    }

    fun selectShutterSpeed(shutterSpeed: ShutterSpeed) {
        _uiState.value = _uiState.value.let { it.copy(draft = it.draft?.copy(shutterSpeed = shutterSpeed)) }
    }

    fun selectAperture(aperture: Double) {
        _uiState.value = _uiState.value.let { it.copy(draft = it.draft?.copy(aperture = aperture)) }
    }

    fun setIso(iso: Int) {
        _uiState.value = _uiState.value.let { it.copy(draft = it.draft?.copy(isoUsed = iso)) }
    }

    fun selectZone(zone: Int) {
        _uiState.value = _uiState.value.let { it.copy(draft = it.draft?.copy(zone = zone)) }
    }

    fun saveEdit() {
        val state = _uiState.value
        val draft = state.draft ?: return
        if (!state.canSave) return

        viewModelScope.launch {
            repository.updateExposure(draft)
            // Best-effort, fire-and-forget — mirrors ExposureEntryViewModel.confirmSave's push after
            // a local save; the edit itself is never lost even if the phone is unreachable.
            exposurePusher.push()
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }
}
