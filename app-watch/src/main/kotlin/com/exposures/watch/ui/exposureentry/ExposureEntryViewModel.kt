package com.exposures.watch.ui.exposureentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.Exposure
import com.exposures.model.Lens
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.watch.sync.CaptureRequestSender
import com.exposures.watch.sync.ExposurePusher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

enum class ExposureEntryStep { PICKERS, CONFIRM }

data class ExposureEntryUiState(
    val isLoading: Boolean = true,
    val lenses: List<Lens> = emptyList(),
    val selectedLensId: String? = null,
    val availableShutterSpeeds: List<ShutterSpeed> = emptyList(),
    val selectedShutterSpeed: ShutterSpeed? = null,
    val availableApertures: List<Double> = emptyList(),
    val selectedAperture: Double? = null,
    val iso: Int = 0,
    val notes: String = "",
    val step: ExposureEntryStep = ExposureEntryStep.PICKERS,
    val savedExposure: Exposure? = null,
) {
    val canConfirm: Boolean
        get() = selectedLensId != null && selectedShutterSpeed != null && selectedAperture != null
}

/** Backs both the picker screen and the confirm screen — see the note in ExposuresNavHost on why they share one ViewModel. */
class ExposureEntryViewModel(
    private val repository: ExposureRepository,
    private val exposurePusher: ExposurePusher,
    private val captureRequestSender: CaptureRequestSender,
    private val rollId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExposureEntryUiState())
    val uiState: StateFlow<ExposureEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val roll = repository.getRoll(rollId)
            val cameraBody = roll?.let { repository.getCameraBody(it.cameraBodyId) }
            val lenses = repository.observeLenses().first()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                lenses = lenses,
                availableShutterSpeeds = cameraBody?.availableShutterSpeeds.orEmpty(),
                iso = roll?.boxSpeedIso ?: 0,
            )
        }
    }

    fun selectLens(lensId: String) {
        val lens = _uiState.value.lenses.find { it.id == lensId } ?: return
        _uiState.value = _uiState.value.copy(
            selectedLensId = lensId,
            availableApertures = lens.availableApertures(),
            selectedAperture = null,
        )
    }

    fun selectShutterSpeed(shutterSpeed: ShutterSpeed) {
        _uiState.value = _uiState.value.copy(selectedShutterSpeed = shutterSpeed)
    }

    fun selectAperture(aperture: Double) {
        _uiState.value = _uiState.value.copy(selectedAperture = aperture)
    }

    fun setIso(iso: Int) {
        _uiState.value = _uiState.value.copy(iso = iso)
    }

    fun setNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun proceedToConfirm() {
        if (!_uiState.value.canConfirm) return
        _uiState.value = _uiState.value.copy(step = ExposureEntryStep.CONFIRM)
    }

    fun backToPickers() {
        _uiState.value = _uiState.value.copy(step = ExposureEntryStep.PICKERS)
    }

    fun confirmSave() {
        val state = _uiState.value
        val lensId = state.selectedLensId ?: return
        val shutterSpeed = state.selectedShutterSpeed ?: return
        val aperture = state.selectedAperture ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val draft = Exposure(
                id = UUID.randomUUID().toString(),
                filmRollId = rollId,
                frameNumber = 0, // resolved by the repository on save
                lensId = lensId,
                shutterSpeed = shutterSpeed,
                aperture = aperture,
                isoUsed = state.iso,
                notes = state.notes.ifBlank { null },
                capturedAt = now,
                referencePhotoStatus = PhotoStatus.NONE,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_SYNC,
                remoteId = null,
            )
            val saved = repository.saveExposure(draft)
            // Fire off the sync/capture signal after the local save succeeds, so a saved exposure
            // is never lost even if the phone is unreachable — send failures just queue in the
            // outbox (see CaptureRequestSender) rather than blocking or losing the save.
            exposurePusher.push()
            captureRequestSender.send(saved.id, saved.filmRollId, saved.frameNumber)
            _uiState.value = _uiState.value.copy(savedExposure = saved)
        }
    }
}
