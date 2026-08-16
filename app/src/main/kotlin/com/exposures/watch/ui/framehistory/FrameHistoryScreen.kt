package com.exposures.watch.ui.framehistory

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.exposures.model.Exposure
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer

@Composable
fun FrameHistoryScreen(rollId: String, onFrameSelected: (String) -> Unit) {
    val container = appContainer()
    val viewModel: FrameHistoryViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.exposurePusher,
            container.captureRequestSender,
            container.rollCompletionSender,
            rollId = rollId,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { Text("Frames") } }
        items(state.exposures) { exposure: Exposure ->
            Chip(
                label = { Text("Frame ${exposure.frameNumber}") },
                secondaryLabel = { Text("${exposure.shutterSpeed.label}  f/${exposure.aperture}  ISO ${exposure.isoUsed}") },
                colors = ChipDefaults.secondaryChipColors(),
                onClick = { onFrameSelected(exposure.id) },
            )
        }
    }
}
