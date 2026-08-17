package com.exposures.watch.ui.rollswitcher

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

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text("Film Rolls") } }

            if (state.isLoading) {
                item { Text("Opening Exposures...") }
                return@TransformingLazyColumn
            }

            item {
                Button(
                    label = { Text(if (state.refreshInFlight) "Refreshing..." else "Refresh from phone") },
                    enabled = !state.refreshInFlight,
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = viewModel::refreshFromPhone,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .fillMaxWidth(),
                )
            }

            if (state.refreshFailed) {
                item { Text("Couldn't reach phone") }
                item {
                    Button(
                        label = { Text("Dismiss") },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        onClick = viewModel::dismissRefreshFailure,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                            .fillMaxWidth(),
                    )
                }
            }

            if (state.rolls.isEmpty()) {
                item { Text("No rolls yet - refresh or add on phone") }
            }

            items(state.rolls) { roll: FilmRoll ->
                Button(
                    label = { Text(roll.name) },
                    secondaryLabel = { Text(roll.filmStock) },
                    colors = if (roll.id == state.activeRollId) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    onClick = {
                        viewModel.selectRoll(roll.id)
                        onRollSelected(roll.id)
                    },
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
