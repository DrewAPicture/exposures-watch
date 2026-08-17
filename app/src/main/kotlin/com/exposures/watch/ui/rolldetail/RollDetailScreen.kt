package com.exposures.watch.ui.rolldetail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
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

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { Text(roll?.name.orEmpty()) }
            item { Text(roll?.let { "${it.filmStock} ${it.boxSpeedIso}" }.orEmpty()) }
            item { Text("Frame ${state.exposureCount}/${roll?.targetFrameCount ?: 0}") }
            item {
                Button(
                    label = { Text("Log Exposure") },
                    enabled = !state.isComplete,
                    colors = ButtonDefaults.buttonColors(),
                    onClick = { onLogExposure(rollId) },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .fillMaxWidth(),
                )
            }
            item {
                Button(
                    label = { Text("History") },
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
