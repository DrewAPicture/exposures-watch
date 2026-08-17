package com.exposures.watch.ui.exposureentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.exposures.model.StandardIso
import com.exposures.model.Zone
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer
import com.exposures.watch.ui.components.PagerEdgeArrows
import com.exposures.watch.ui.components.ValuePickerRow

private enum class EntryPage { HISTORY, LENS, SHUTTER_SPEED, APERTURE, ISO, ZONE, CAPTURE }

@Composable
fun ExposureEntryScreen(rollId: String, onSaved: () -> Unit, onRollCompleted: () -> Unit, onViewHistory: (String) -> Unit) {
    val container = appContainer()
    val viewModel: ExposureEntryViewModel = viewModel(
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

    // Only the plain "logged a non-final frame" path pops back on its own — filling the last frame
    // routes through rollCompleted instead (see ExposureEntryViewModel.confirmSave), and a failed
    // completion attempt stays on this screen so the user can retry.
    LaunchedEffect(state.savedExposure, state.isLastFrame) {
        if (state.savedExposure != null && !state.isLastFrame) onSaved()
    }
    LaunchedEffect(state.rollCompleted) {
        if (state.rollCompleted) onRollCompleted()
    }

    val pages = remember(state.showZonePicker) {
        buildList {
            add(EntryPage.HISTORY)
            add(EntryPage.LENS)
            add(EntryPage.SHUTTER_SPEED)
            add(EntryPage.APERTURE)
            add(EntryPage.ISO)
            if (state.showZonePicker) add(EntryPage.ZONE)
            add(EntryPage.CAPTURE)
        }
    }
    val pagerState = rememberPagerState(initialPage = pages.indexOf(EntryPage.LENS)) { pages.size }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(
                state = pagerState,
                flingBehavior = PagerDefaults.snapFlingBehavior(
                    state = pagerState,
                    maxFlingPages = 1,
                    snapPositionalThreshold = PagerScaffoldDefaults.HighSnapPositionalThreshold,
                    snapAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                ),
                rotaryScrollableBehavior = null,
            ) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                    ScreenScaffold {
                        EntryPageContent(
                            page = pages[page],
                            rollId = rollId,
                            state = state,
                            viewModel = viewModel,
                            onViewHistory = onViewHistory,
                        )
                    }
                }
            }
        }
        PagerEdgeArrows(pagerState = pagerState)
    }
}

@Composable
private fun EntryPageContent(
    page: EntryPage,
    rollId: String,
    state: ExposureEntryUiState,
    viewModel: ExposureEntryViewModel,
    onViewHistory: (String) -> Unit,
) {
    when (page) {
        EntryPage.HISTORY ->
            CenteredPage {
                Button(
                    label = { Text("History", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = { onViewHistory(rollId) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        EntryPage.LENS -> {
            val lensIndex = state.lenses.indexOfFirst { it.id == state.selectedLensId }
            CenteredPage {
                ValuePickerRow(
                    label = "Lens",
                    items = state.lenses.map { it.name },
                    selectedIndex = lensIndex,
                    onSelectedIndexChange = { index -> state.lenses.getOrNull(index)?.let { viewModel.selectLens(it.id) } },
                )
            }
        }
        EntryPage.SHUTTER_SPEED -> {
            val shutterIndex = state.availableShutterSpeeds.indexOf(state.selectedShutterSpeed)
            CenteredPage {
                ValuePickerRow(
                    label = "Shutter Speed",
                    items = state.availableShutterSpeeds.map { it.label },
                    selectedIndex = shutterIndex,
                    onSelectedIndexChange = { index ->
                        state.availableShutterSpeeds.getOrNull(index)?.let(viewModel::selectShutterSpeed)
                    },
                )
            }
        }
        EntryPage.APERTURE -> {
            val apertureIndex = state.availableApertures.indexOf(state.selectedAperture)
            CenteredPage {
                ValuePickerRow(
                    label = "Aperture",
                    items = state.availableApertures.map { "ƒ/$it" },
                    selectedIndex = apertureIndex,
                    onSelectedIndexChange = { index -> state.availableApertures.getOrNull(index)?.let(viewModel::selectAperture) },
                )
            }
        }
        EntryPage.ISO -> {
            val isoIndex = StandardIso.FULL_STOP_SCALE.indexOf(state.iso)
            CenteredPage {
                ValuePickerRow(
                    label = "ISO",
                    items = StandardIso.FULL_STOP_SCALE.map { it.toString() },
                    selectedIndex = isoIndex,
                    onSelectedIndexChange = { index -> StandardIso.FULL_STOP_SCALE.getOrNull(index)?.let(viewModel::setIso) },
                )
            }
        }
        EntryPage.ZONE -> {
            val zoneRange = (Zone.MIN..Zone.MAX).toList()
            CenteredPage {
                ValuePickerRow(
                    label = "Zone",
                    items = zoneRange.map(Zone::label),
                    selectedIndex = state.selectedZone ?: -1,
                    onSelectedIndexChange = { index -> zoneRange.getOrNull(index)?.let(viewModel::selectZone) },
                )
            }
        }
        EntryPage.CAPTURE -> CapturePage(state = state, viewModel = viewModel)
    }
}

@Composable
private fun CenteredPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun CapturePage(state: ExposureEntryUiState, viewModel: ExposureEntryViewModel) {
    CenteredPage {
        if (state.completeRollFailed) {
            Text(text = "Couldn't reach phone — try again", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Button(
                label = { Text("Retry", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                colors = ButtonDefaults.buttonColors(),
                onClick = viewModel::retryCompleteRoll,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                label = { Text("Dismiss", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                colors = ButtonDefaults.filledTonalButtonColors(),
                onClick = viewModel::dismissCompleteRollFailure,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Button(
                label = {
                    Text(
                        if (state.isLastFrame) "Complete Roll" else "Capture",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                enabled = state.canConfirm,
                colors = ButtonDefaults.buttonColors(),
                onClick = viewModel::confirmSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
