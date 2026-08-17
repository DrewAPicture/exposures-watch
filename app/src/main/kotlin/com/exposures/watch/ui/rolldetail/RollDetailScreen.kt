package com.exposures.watch.ui.rolldetail

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
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
            container.rollCompletionSender,
            container.rollsSyncRequestSender,
            rollId = rollId,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    val roll = state.roll

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = { onLogExposure(rollId) },
                enabled = !state.isComplete,
                modifier = Modifier.scrollable(
                    listState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect(),
                ),
            ) {
                Text("Log Exposure")
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { Text(roll?.name.orEmpty()) }
            item { Text(roll?.filmStock.orEmpty()) }
            item { Text("Frame ${state.exposureCount}/${roll?.targetFrameCount ?: 0}") }
            item { Text(state.cameraBody?.name.orEmpty()) }
            item {
                Button(
                    label = { Text("Frame History") },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = { onViewHistory(rollId) },
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
