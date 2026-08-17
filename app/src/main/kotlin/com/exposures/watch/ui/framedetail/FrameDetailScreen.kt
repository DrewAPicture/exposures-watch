package com.exposures.watch.ui.framedetail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer

@Composable
fun FrameDetailScreen(exposureId: String, onEdit: (String) -> Unit) {
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

    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            if (exposure == null) {
                item { Text("Frame not found") }
                return@TransformingLazyColumn
            }
            item { Text("Frame ${exposure.frameNumber}") }
            item { Text(state.lens?.name.orEmpty()) }
            item { Text(exposure.shutterSpeed.label) }
            item { Text("ƒ/${exposure.aperture}") }
            item { Text("ISO ${exposure.isoUsed}") }
            exposure.notes?.let { notes -> item { Text(notes) } }
            item {
                Button(
                    label = { Text("Edit", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = { onEdit(exposureId) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
