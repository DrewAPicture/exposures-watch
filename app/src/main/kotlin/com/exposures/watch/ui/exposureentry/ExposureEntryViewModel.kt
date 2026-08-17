package com.exposures.watch.ui.exposureentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.Exposure
import com.exposures.model.Lens
import com.exposures.model.LightMeterType
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.model.Zone
import com.exposures.watch.sync.CaptureRequestSender
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.RollCompletionSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class ExposureEntryUiState(
    val isLoading: Boolean = true,
    val lenses: List<Lens> = emptyList(),
    val selectedLensId: String? = null,
    val availableShutterSpeeds: List<ShutterSpeed> = emptyList(),
    val selectedShutterSpeed: ShutterSpeed? = null,
    val availableApertures: List<Double> = emptyList(),
    val selectedAperture: Double? = null,
    val iso: Int = 0,
    val showZonePicker: Boolean = false,
    val selectedZone: Int? = null,
    val notes: String = "",
    val savedExposure: Exposure? = null,
    val showCompleteRollConfirmation: Boolean = false,
    val rollCompleted: Boolean = false,
    val completeRollFailed: Boolean = false,
) {
    val canConfirm: Boolean
        get() = selectedLensId != null && selectedShutterSpeed != null && selectedAperture != null &&
            (!showZonePicker || selectedZone != null)
}

class ExposureEntryViewModel(
    private val repository: ExposureRepository,
    private val exposurePusher: ExposurePusher,
    private val captureRequestSender: CaptureRequestSender,
    private val rollCompletionSender: RollCompletionSender,
    private val rollId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExposureEntryUiState())
    val uiState: StateFlow<ExposureEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val roll = repository.getRoll(rollId)
            val cameraBody = roll?.let { repository.getCameraBody(it.cameraBodyId) }
            val lenses = repository.observeLenses().first()
                .filter { lens -> lens.cameraBodyId == null || lens.cameraBodyId == roll?.cameraBodyId }
            val lastUsed = repository.observeLastUsedExposureSettings().first()

            val lightMeter = roll?.lightMeterId?.let { repository.getLightMeter(it) }
            val showZonePicker = lightMeter?.type == LightMeterType.SPOT

            val availableShutterSpeeds = cameraBody?.availableShutterSpeeds.orEmpty()
            // Only carry a last-used value over if it's still valid for *this* roll's equipment —
            // a different roll can mean a different camera body (different shutter speeds) even
            // though lenses aren't roll-specific. Falls back to the first available option rather
            // than leaving a field unselected, so Capture is submittable without having to touch
            // anything first (e.g. logging several frames in a row with identical settings).
            val selectedLens = lastUsed.lensId?.let { id -> lenses.find { it.id == id } } ?: lenses.firstOrNull()
            val availableApertures = selectedLens?.availableApertures().orEmpty()
            val selectedShutterSpeed = lastUsed.shutterSpeed?.takeIf { it in availableShutterSpeeds }
                ?: availableShutterSpeeds.firstOrNull()
            val selectedAperture = lastUsed.aperture?.takeIf { it in availableApertures } ?: availableApertures.firstOrNull()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                lenses = lenses,
                selectedLensId = selectedLens?.id,
                availableShutterSpeeds = availableShutterSpeeds,
                selectedShutterSpeed = selectedShutterSpeed,
                availableApertures = availableApertures,
                selectedAperture = selectedAperture,
                iso = lastUsed.iso ?: roll?.boxSpeedIso ?: 0,
                showZonePicker = showZonePicker,
                // Zone VI is the fixed starting point the first time the picker is ever shown;
                // after that, whatever was last chosen carries forward (see AppStateDao's COALESCE).
                selectedZone = if (showZonePicker) (lastUsed.zone ?: Zone.DEFAULT) else null,
            )
        }
    }

    fun selectLens(lensId: String) {
        val lens = _uiState.value.lenses.find { it.id == lensId } ?: return
        val availableApertures = lens.availableApertures()
        _uiState.value = _uiState.value.copy(
            selectedLensId = lensId,
            availableApertures = availableApertures,
            selectedAperture = availableApertures.firstOrNull(),
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

    fun selectZone(zone: Int) {
        _uiState.value = _uiState.value.copy(selectedZone = zone)
    }

    fun setNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun confirmSave() {
        val state = _uiState.value
        if (!state.canConfirm) return
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
                zone = state.selectedZone,
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

    /** Long-press on the capture control opens this confirmation rather than completing immediately. */
    fun requestCompleteRoll() {
        _uiState.value = _uiState.value.copy(showCompleteRollConfirmation = true)
    }

    fun cancelCompleteRoll() {
        _uiState.value = _uiState.value.copy(showCompleteRollConfirmation = false)
    }

    fun confirmCompleteRoll() {
        viewModelScope.launch {
            val sent = rollCompletionSender.complete(rollId)
            _uiState.value = if (sent) {
                _uiState.value.copy(showCompleteRollConfirmation = false, rollCompleted = true)
            } else {
                _uiState.value.copy(showCompleteRollConfirmation = false, completeRollFailed = true)
            }
        }
    }

    fun dismissCompleteRollFailure() {
        _uiState.value = _uiState.value.copy(completeRollFailed = false)
    }
}
