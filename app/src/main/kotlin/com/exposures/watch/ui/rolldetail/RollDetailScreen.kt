package com.exposures.watch.ui.rolldetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
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

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(roll?.name.orEmpty())
            Text(roll?.let { "${it.filmStock} ${it.boxSpeedIso}" }.orEmpty())
            Text("Frame ${state.exposureCount}/${roll?.targetFrameCount ?: 0}")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    label = { Text("Log Exposure") },
                    enabled = !state.isComplete,
                    colors = ButtonDefaults.buttonColors(),
                    onClick = { onLogExposure(rollId) },
                    modifier = Modifier.weight(3f),
                )
                Button(
                    label = { Text("?") },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = { onViewHistory(rollId) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
