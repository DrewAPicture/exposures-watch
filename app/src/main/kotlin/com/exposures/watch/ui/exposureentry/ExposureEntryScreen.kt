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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.exposures.model.ExposureValue
import com.exposures.model.StandardIso
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer
import com.exposures.watch.ui.components.PagerEdgeArrows
import com.exposures.watch.ui.components.ValuePickerRow
import kotlinx.coroutines.launch

private enum class EntryPage { QUICK_CAPTURE, LENS, FOCAL_LENGTH, SHUTTER_SPEED, APERTURE, ISO, EXPOSURE_VALUE, CAPTURE }

@Composable
fun ExposureEntryScreen(filmMediumId: String, onSaved: () -> Unit, onFilmMediumCompleted: () -> Unit) {
    val container = appContainer()
    val viewModel: ExposureEntryViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.exposurePusher,
            container.filmMediumCompletionSender,
            container.filmMediaSyncRequestSender,
            filmMediumId = filmMediumId,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    // Only the plain "logged a non-final frame" path pops back on its own — filling the last frame
    // routes through filmMediumCompleted instead (see ExposureEntryViewModel.confirmSave), and a
    // failed completion attempt stays on this screen so the user can retry.
    LaunchedEffect(state.savedExposure, state.isLastFrame) {
        if (state.savedExposure != null && !state.isLastFrame) onSaved()
    }
    LaunchedEffect(state.filmMediumCompleted) {
        if (state.filmMediumCompleted) onFilmMediumCompleted()
    }

    val pages = remember(state.showExposureValuePicker, state.showFocalLengthPicker) {
        buildList {
            add(EntryPage.QUICK_CAPTURE)
            add(EntryPage.LENS)
            if (state.showFocalLengthPicker) add(EntryPage.FOCAL_LENGTH)
            add(EntryPage.SHUTTER_SPEED)
            add(EntryPage.APERTURE)
            add(EntryPage.ISO)
            if (state.showExposureValuePicker) add(EntryPage.EXPOSURE_VALUE)
            add(EntryPage.CAPTURE)
        }
    }
    val pagerState = rememberPagerState(initialPage = pages.indexOf(EntryPage.LENS)) { pages.size }
    val coroutineScope = rememberCoroutineScope()

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
                            state = state,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
        PagerEdgeArrows(
            pagerState = pagerState,
            onLongClickRight = { coroutineScope.launch { pagerState.animateScrollToPage(pages.lastIndex) } },
            onLongClickRightLabel = "Skip to Capture",
        )
    }
}

@Composable
private fun EntryPageContent(
    page: EntryPage,
    state: ExposureEntryUiState,
    viewModel: ExposureEntryViewModel,
) {
    when (page) {
        EntryPage.QUICK_CAPTURE -> CapturePage(state = state, viewModel = viewModel)
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
        EntryPage.FOCAL_LENGTH -> {
            val focalLengthIndex = state.availableFocalLengths.indexOf(state.selectedFocalLengthMm)
            CenteredPage {
                ValuePickerRow(
                    label = "Focal Length",
                    items = state.availableFocalLengths.map { "${it}mm" },
                    selectedIndex = focalLengthIndex,
                    onSelectedIndexChange = { index ->
                        state.availableFocalLengths.getOrNull(index)?.let(viewModel::selectFocalLength)
                    },
                )
            }
        }
        EntryPage.SHUTTER_SPEED -> {
            // Reversed to slowest-first so swiping up moves toward faster speeds — the Picker
            // walks toward higher indices on an upward swipe, and availableShutterSpeeds itself
            // stays fastest-first for the last-used-value defaulting logic in the view model.
            val shutterSpeeds = state.availableShutterSpeeds.asReversed()
            val shutterIndex = shutterSpeeds.indexOf(state.selectedShutterSpeed)
            CenteredPage {
                ValuePickerRow(
                    label = "Shutter Speed",
                    items = shutterSpeeds.map { it.label },
                    selectedIndex = shutterIndex,
                    onSelectedIndexChange = { index ->
                        shutterSpeeds.getOrNull(index)?.let(viewModel::selectShutterSpeed)
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
        EntryPage.EXPOSURE_VALUE -> {
            val exposureValueRange = (ExposureValue.MIN..ExposureValue.MAX).toList()
            CenteredPage {
                ValuePickerRow(
                    label = "EV",
                    items = exposureValueRange.map(ExposureValue::label),
                    selectedIndex = exposureValueRange.indexOf(state.selectedExposureValue),
                    onSelectedIndexChange = { index -> exposureValueRange.getOrNull(index)?.let(viewModel::selectExposureValue) },
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
        if (state.completeFilmMediumFailed) {
            Text(text = "Couldn't reach phone — try again", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Button(
                label = { Text("Retry", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                colors = ButtonDefaults.buttonColors(),
                onClick = viewModel::retryCompleteFilmMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                label = { Text("Dismiss", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                colors = ButtonDefaults.filledTonalButtonColors(),
                onClick = viewModel::dismissCompleteFilmMediumFailure,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Button(
                label = {
                    Text(
                        if (state.isLastFrame) "Complete Film" else "Capture",
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
