package com.exposures.watch.ui.frameedit

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
import com.exposures.model.StandardIso
import com.exposures.model.Zone
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer
import com.exposures.watch.ui.components.PagerEdgeArrows
import com.exposures.watch.ui.components.ValuePickerRow
import kotlinx.coroutines.launch

private enum class EditPage { LENS, FOCAL_LENGTH, SHUTTER_SPEED, APERTURE, ISO, ZONE, SAVE }

@Composable
fun FrameEditScreen(exposureId: String, onSaved: () -> Unit) {
    val container = appContainer()
    val viewModel: FrameEditViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.exposurePusher,
            container.filmMediumCompletionSender,
            container.filmMediaSyncRequestSender,
            exposureId = exposureId,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    val pages = remember(state.showZonePicker, state.showFocalLengthPicker) {
        buildList {
            add(EditPage.LENS)
            if (state.showFocalLengthPicker) add(EditPage.FOCAL_LENGTH)
            add(EditPage.SHUTTER_SPEED)
            add(EditPage.APERTURE)
            add(EditPage.ISO)
            if (state.showZonePicker) add(EditPage.ZONE)
            add(EditPage.SAVE)
        }
    }
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
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
                        EditPageContent(page = pages[page], state = state, viewModel = viewModel)
                    }
                }
            }
        }
        PagerEdgeArrows(
            pagerState = pagerState,
            onLongClickRight = { coroutineScope.launch { pagerState.animateScrollToPage(pages.lastIndex) } },
            onLongClickRightLabel = "Skip to Save",
        )
    }
}

@Composable
private fun EditPageContent(page: EditPage, state: FrameEditUiState, viewModel: FrameEditViewModel) {
    val draft = state.draft
    when (page) {
        EditPage.LENS -> {
            val lensIndex = state.lenses.indexOfFirst { it.id == draft?.lensId }
            CenteredPage {
                ValuePickerRow(
                    label = "Lens",
                    items = state.lenses.map { it.name },
                    selectedIndex = lensIndex,
                    onSelectedIndexChange = { index -> state.lenses.getOrNull(index)?.let { viewModel.selectLens(it.id) } },
                )
            }
        }
        EditPage.FOCAL_LENGTH -> {
            val focalLengthIndex = state.availableFocalLengths.indexOf(draft?.focalLengthMm)
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
        EditPage.SHUTTER_SPEED -> {
            val shutterIndex = state.availableShutterSpeeds.indexOf(draft?.shutterSpeed)
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
        EditPage.APERTURE -> {
            val apertureIndex = state.availableApertures.indexOf(draft?.aperture)
            CenteredPage {
                ValuePickerRow(
                    label = "Aperture",
                    items = state.availableApertures.map { "ƒ/$it" },
                    selectedIndex = apertureIndex,
                    onSelectedIndexChange = { index -> state.availableApertures.getOrNull(index)?.let(viewModel::selectAperture) },
                )
            }
        }
        EditPage.ISO -> {
            val isoIndex = StandardIso.FULL_STOP_SCALE.indexOf(draft?.isoUsed)
            CenteredPage {
                ValuePickerRow(
                    label = "ISO",
                    items = StandardIso.FULL_STOP_SCALE.map { it.toString() },
                    selectedIndex = isoIndex,
                    onSelectedIndexChange = { index -> StandardIso.FULL_STOP_SCALE.getOrNull(index)?.let(viewModel::setIso) },
                )
            }
        }
        EditPage.ZONE -> {
            val zoneRange = (Zone.MIN..Zone.MAX).toList()
            CenteredPage {
                ValuePickerRow(
                    label = "Zone",
                    items = zoneRange.map(Zone::label),
                    selectedIndex = draft?.zone ?: -1,
                    onSelectedIndexChange = { index -> zoneRange.getOrNull(index)?.let(viewModel::selectZone) },
                )
            }
        }
        EditPage.SAVE ->
            CenteredPage {
                Button(
                    label = { Text("Save Edits", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    enabled = state.canSave,
                    colors = ButtonDefaults.buttonColors(),
                    onClick = viewModel::saveEdit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
