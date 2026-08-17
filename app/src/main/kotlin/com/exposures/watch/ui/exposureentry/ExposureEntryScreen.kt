package com.exposures.watch.ui.exposureentry

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.exposures.model.StandardIso
import com.exposures.model.Zone
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer
import com.exposures.watch.ui.components.ValuePickerRow

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

    if (state.showCompleteRollConfirmation) {
        CompleteRollConfirmationContent(onConfirm = viewModel::confirmCompleteRoll, onCancel = viewModel::cancelCompleteRoll)
    } else {
        ExposureEntryContent(state = state, viewModel = viewModel)
    }
}

@Composable
private fun ExposureEntryContent(state: ExposureEntryUiState, viewModel: ExposureEntryViewModel) {
    val lensIndex = state.lenses.indexOfFirst { it.id == state.selectedLensId }
    val shutterIndex = state.availableShutterSpeeds.indexOf(state.selectedShutterSpeed)
    val apertureIndex = state.availableApertures.indexOf(state.selectedAperture)
    val isoIndex = StandardIso.FULL_STOP_SCALE.indexOf(state.iso)
    val zoneRange = (Zone.MIN..Zone.MAX).toList()

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = viewModel::confirmSave,
                enabled = state.canConfirm,
                modifier = Modifier.scrollable(
                    listState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect(),
                ),
            ) {
                Text("Capture")
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ValuePickerRow(
                    label = "Lens",
                    items = state.lenses.map { it.name },
                    selectedIndex = lensIndex,
                    onSelectedIndexChange = { index -> state.lenses.getOrNull(index)?.let { viewModel.selectLens(it.id) } },
                )
            }
            item {
                ValuePickerRow(
                    label = "Shutter Speed",
                    items = state.availableShutterSpeeds.map { it.label },
                    selectedIndex = shutterIndex,
                    onSelectedIndexChange = { index ->
                        state.availableShutterSpeeds.getOrNull(index)?.let(viewModel::selectShutterSpeed)
                    },
                )
            }
            item {
                ValuePickerRow(
                    label = "Aperture",
                    items = state.availableApertures.map { "ƒ/$it" },
                    selectedIndex = apertureIndex,
                    onSelectedIndexChange = { index -> state.availableApertures.getOrNull(index)?.let(viewModel::selectAperture) },
                )
            }
            item {
                ValuePickerRow(
                    label = "ISO",
                    items = StandardIso.FULL_STOP_SCALE.map { it.toString() },
                    selectedIndex = isoIndex,
                    onSelectedIndexChange = { index -> StandardIso.FULL_STOP_SCALE.getOrNull(index)?.let(viewModel::setIso) },
                )
            }
            if (state.showZonePicker) {
                item {
                    ValuePickerRow(
                        label = "Zone",
                        items = zoneRange.map(Zone::label),
                        selectedIndex = state.selectedZone ?: -1,
                        onSelectedIndexChange = { index -> zoneRange.getOrNull(index)?.let(viewModel::selectZone) },
                    )
                }
            }
            if (state.completeRollFailed) {
                item { Text("Couldn't reach phone — try again") }
                item {
                    Button(
                        label = { Text("Dismiss") },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        onClick = viewModel::dismissCompleteRollFailure,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                            .fillMaxWidth(),
                    )
                }
            }
            item {
                Button(
                    label = { Text("Complete Roll") },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = viewModel::requestCompleteRoll,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CompleteRollConfirmationContent(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { Text("Complete this roll?") }
            item {
                Button(
                    label = { Text("Yes, complete roll") },
                    colors = ButtonDefaults.buttonColors(),
                    onClick = onConfirm,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .fillMaxWidth(),
                )
            }
            item {
                Button(
                    label = { Text("Cancel") },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = onCancel,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
