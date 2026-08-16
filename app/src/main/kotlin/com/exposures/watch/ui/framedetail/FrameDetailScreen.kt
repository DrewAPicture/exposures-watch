package com.exposures.watch.ui.framedetail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer

@Composable
fun FrameDetailScreen(exposureId: String) {
    val container = appContainer()
    val viewModel: FrameDetailViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.exposurePusher,
            container.captureRequestSender,
            container.rollCompletionSender,
            container.rollsSyncRequestSender,
            exposureId = exposureId,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    val exposure = state.exposure

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        if (exposure == null) {
            item { Text("Frame not found") }
            return@ScalingLazyColumn
        }
        item { Text("Frame ${exposure.frameNumber}") }
        item { Text(state.lens?.name.orEmpty()) }
        item { Text(exposure.shutterSpeed.label) }
        item { Text("ƒ/${exposure.aperture}") }
        item { Text("ISO ${exposure.isoUsed}") }
        exposure.notes?.let { notes -> item { Text(notes) } }
    }
}
