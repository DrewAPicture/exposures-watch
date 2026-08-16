package com.exposures.watch.ui.rolldetail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer

@Composable
fun RollDetailScreen(
    rollId: String,
    onLogExposure: (String) -> Unit,
    onViewHistory: (String) -> Unit,
) {
    val container = appContainer()
    val viewModel: RollDetailViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.exposurePusher,
            container.captureRequestSender,
            rollId = rollId,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    val roll = state.roll

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Text(roll?.name.orEmpty()) }
        item { Text(roll?.filmStock.orEmpty()) }
        item { Text("Frame ${state.exposureCount}/${roll?.targetFrameCount ?: 0}") }
        item { Text(state.cameraBody?.name.orEmpty()) }
        item {
            Chip(
                label = { Text("Log Exposure") },
                enabled = !state.isComplete,
                colors = ChipDefaults.primaryChipColors(),
                onClick = { onLogExposure(rollId) },
            )
        }
        item {
            Chip(
                label = { Text("Frame History") },
                colors = ChipDefaults.secondaryChipColors(),
                onClick = { onViewHistory(rollId) },
            )
        }
    }
}
