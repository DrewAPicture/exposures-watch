package com.exposures.watch.ui.rollswitcher

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
import com.exposures.model.FilmRoll
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer

@Composable
fun RollSwitcherScreen(onRollSelected: (String) -> Unit) {
    val container = appContainer()
    val viewModel: RollSwitcherViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.exposurePusher,
            container.captureRequestSender,
            container.rollCompletionSender,
            container.rollsSyncRequestSender,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { Text("Film Rolls") } }
        if (state.isLoading) {
            item { Text("Opening Exposures...") }
            return@ScalingLazyColumn
        }
        item {
            Chip(
                label = { Text(if (state.refreshInFlight) "Refreshing..." else "Refresh from phone") },
                enabled = !state.refreshInFlight,
                colors = ChipDefaults.secondaryChipColors(),
                onClick = viewModel::refreshFromPhone,
            )
        }
        if (state.refreshFailed) {
            item { Text("Couldn't reach phone") }
            item {
                Chip(
                    label = { Text("Dismiss") },
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = viewModel::dismissRefreshFailure,
                )
            }
        }
        if (state.rolls.isEmpty()) {
            item { Text("No rolls yet - refresh or add on phone") }
        }
        items(state.rolls) { roll: FilmRoll ->
            Chip(
                label = { Text(roll.name) },
                secondaryLabel = { Text(roll.filmStock) },
                colors = if (roll.id == state.activeRollId) {
                    ChipDefaults.primaryChipColors()
                } else {
                    ChipDefaults.secondaryChipColors()
                },
                onClick = {
                    viewModel.selectRoll(roll.id)
                    onRollSelected(roll.id)
                },
            )
        }
    }
}
