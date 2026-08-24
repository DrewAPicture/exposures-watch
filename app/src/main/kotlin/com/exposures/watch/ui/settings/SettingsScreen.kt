package com.exposures.watch.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer

@Composable
fun SettingsScreen() {
    val container = appContainer()
    val viewModel: WatchSettingsViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            repository = container.repository,
            exposurePusher = container.exposurePusher,
            rollCompletionSender = container.rollCompletionSender,
            rollsSyncRequestSender = container.rollsSyncRequestSender,
            offlineModePreferences = container.offlineModePreferences,
            offlineModeQueueFlusher = container.offlineModeQueueFlusher,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text("Settings") } }
            item {
                SwitchButton(
                    checked = state.offlineModeEnabled,
                    onCheckedChange = viewModel::setOfflineModeEnabled,
                    label = { Text("Offline Mode") },
                    secondaryLabel = { Text("Pause phone/watch sync and command traffic.") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
