package com.exposures.watch.ui.framehistory

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
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
            container.rollsSyncRequestSender,
            rollId = rollId,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text("Frames") } }
            items(state.exposures) { exposure: Exposure ->
                Button(
                    label = { Text("Frame ${exposure.frameNumber}") },
                    secondaryLabel = { Text("${exposure.shutterSpeed.label}  f/${exposure.aperture}  ISO ${exposure.isoUsed}") },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = { onFrameSelected(exposure.id) },
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
