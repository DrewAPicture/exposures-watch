package com.exposures.watch.ui.exposureentry

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.exposures.model.StandardIso
import com.exposures.model.Zone
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer
import com.exposures.watch.ui.components.ValueStepperRow

@Composable
fun ExposureEntryScreen(rollId: String, onSaved: () -> Unit, onRollCompleted: () -> Unit) {
    val container = appContainer()
    val viewModel: ExposureEntryViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.exposurePusher,
            container.captureRequestSender,
            container.rollCompletionSender,
            container.rollsSyncRequestSender,
            rollId = rollId,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.savedExposure) {
        if (state.savedExposure != null) onSaved()
    }
    LaunchedEffect(state.rollCompleted) {
        if (state.rollCompleted) onRollCompleted()
    }

    if (state.step == ExposureEntryStep.CONFIRM) {
        ConfirmExposureContent(
            state = state,
            onEdit = viewModel::backToPickers,
            onSave = viewModel::confirmSave,
            onRequestCompleteRoll = viewModel::requestCompleteRoll,
            onConfirmCompleteRoll = viewModel::confirmCompleteRoll,
            onCancelCompleteRoll = viewModel::cancelCompleteRoll,
            onDismissCompleteRollFailure = viewModel::dismissCompleteRollFailure,
        )
    } else {
        ExposurePickersContent(state = state, viewModel = viewModel)
    }
}

@Composable
private fun ExposurePickersContent(state: ExposureEntryUiState, viewModel: ExposureEntryViewModel) {
    val lensIndex = state.lenses.indexOfFirst { it.id == state.selectedLensId }
    val shutterIndex = state.availableShutterSpeeds.indexOf(state.selectedShutterSpeed)
    val apertureIndex = state.availableApertures.indexOf(state.selectedAperture)
    val isoIndex = StandardIso.FULL_STOP_SCALE.indexOf(state.iso)

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ValueStepperRow(
                label = "Lens",
                value = state.lenses.getOrNull(lensIndex)?.name ?: "Select lens",
                hasPrevious = lensIndex > 0,
                hasNext = lensIndex < state.lenses.lastIndex,
                onPrevious = { state.lenses.getOrNull(lensIndex - 1)?.let { viewModel.selectLens(it.id) } },
                onNext = {
                    val next = if (lensIndex == -1) state.lenses.firstOrNull() else state.lenses.getOrNull(lensIndex + 1)
                    next?.let { viewModel.selectLens(it.id) }
                },
            )
        }
        item {
            ValueStepperRow(
                label = "Shutter Speed",
                value = state.selectedShutterSpeed?.label ?: "Select speed",
                hasPrevious = shutterIndex > 0,
                hasNext = shutterIndex < state.availableShutterSpeeds.lastIndex,
                onPrevious = { state.availableShutterSpeeds.getOrNull(shutterIndex - 1)?.let(viewModel::selectShutterSpeed) },
                onNext = {
                    val next = if (shutterIndex == -1) {
                        state.availableShutterSpeeds.firstOrNull()
                    } else {
                        state.availableShutterSpeeds.getOrNull(shutterIndex + 1)
                    }
                    next?.let(viewModel::selectShutterSpeed)
                },
            )
        }
        item {
            ValueStepperRow(
                label = "Aperture",
                value = state.selectedAperture?.let { "ƒ/$it" } ?: "Select aperture",
                hasPrevious = apertureIndex > 0,
                hasNext = apertureIndex < state.availableApertures.lastIndex,
                onPrevious = { state.availableApertures.getOrNull(apertureIndex - 1)?.let(viewModel::selectAperture) },
                onNext = {
                    val next = if (apertureIndex == -1) {
                        state.availableApertures.firstOrNull()
                    } else {
                        state.availableApertures.getOrNull(apertureIndex + 1)
                    }
                    next?.let(viewModel::selectAperture)
                },
            )
        }
        item {
            ValueStepperRow(
                label = "ISO",
                value = state.iso.toString(),
                hasPrevious = isoIndex > 0,
                hasNext = isoIndex < StandardIso.FULL_STOP_SCALE.lastIndex,
                onPrevious = { StandardIso.FULL_STOP_SCALE.getOrNull(isoIndex - 1)?.let(viewModel::setIso) },
                onNext = { StandardIso.FULL_STOP_SCALE.getOrNull(isoIndex + 1)?.let(viewModel::setIso) },
            )
        }
        if (state.showZonePicker) {
            item {
                ValueStepperRow(
                    label = "Zone",
                    value = state.selectedZone?.let(Zone::label) ?: "Select zone",
                    hasPrevious = (state.selectedZone ?: Zone.MIN) > Zone.MIN,
                    hasNext = (state.selectedZone ?: Zone.MIN) < Zone.MAX,
                    onPrevious = { state.selectedZone?.let { viewModel.selectZone(it - 1) } },
                    onNext = { viewModel.selectZone((state.selectedZone ?: (Zone.MIN - 1)) + 1) },
                )
            }
        }
        item {
            Chip(
                label = { Text("Review") },
                enabled = state.canConfirm,
                colors = ChipDefaults.primaryChipColors(),
                onClick = viewModel::proceedToConfirm,
            )
        }
    }
}

@Composable
private fun ConfirmExposureContent(
    state: ExposureEntryUiState,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onRequestCompleteRoll: () -> Unit,
    onConfirmCompleteRoll: () -> Unit,
    onCancelCompleteRoll: () -> Unit,
    onDismissCompleteRollFailure: () -> Unit,
) {
    if (state.showCompleteRollConfirmation) {
        CompleteRollConfirmationContent(onConfirm = onConfirmCompleteRoll, onCancel = onCancelCompleteRoll)
        return
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Text("Confirm Exposure") }
        item { Text(state.selectedShutterSpeed?.label.orEmpty()) }
        item { Text(state.selectedAperture?.let { "ƒ/$it" }.orEmpty()) }
        item { Text("ISO ${state.iso}") }
        if (state.showZonePicker) {
            item { Text("Zone ${state.selectedZone?.let(Zone::label).orEmpty()}") }
        }
        if (state.completeRollFailed) {
            item { Text("Couldn't reach phone — try again") }
            item {
                Chip(
                    label = { Text("Dismiss") },
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = onDismissCompleteRollFailure,
                )
            }
        }
        item {
            // Tap saves the exposure and triggers capture; long-press (then confirm) marks the
            // roll complete instead — Chip has no built-in long-click, so this is a custom surface.
            LongPressableActionSurface(label = "Save", onClick = onSave, onLongClick = onRequestCompleteRoll)
        }
        item {
            Chip(
                label = { Text("Edit") },
                colors = ChipDefaults.secondaryChipColors(),
                onClick = onEdit,
            )
        }
    }
}

@Composable
private fun CompleteRollConfirmationContent(onConfirm: () -> Unit, onCancel: () -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Text("Complete this roll?") }
        item {
            Chip(label = { Text("Yes, complete roll") }, colors = ChipDefaults.primaryChipColors(), onClick = onConfirm)
        }
        item {
            Chip(label = { Text("Cancel") }, colors = ChipDefaults.secondaryChipColors(), onClick = onCancel)
        }
    }
}

@Composable
private fun LongPressableActionSurface(label: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colors.primary)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = MaterialTheme.colors.onPrimary)
    }
}
