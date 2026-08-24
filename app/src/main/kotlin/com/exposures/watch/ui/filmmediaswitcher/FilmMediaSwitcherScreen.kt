package com.exposures.watch.ui.filmmediaswitcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.exposures.model.FilmMediumStatus
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer
import com.exposures.watch.ui.components.PagerEdgeArrows

private sealed interface SwitcherPage {
    data class FilmMediumPage(val filmMediumId: String) : SwitcherPage
    data object Refresh : SwitcherPage
    data object Empty : SwitcherPage
}

@Composable
fun FilmMediaSwitcherScreen(onFilmMediumSelected: (String) -> Unit) {
    val container = appContainer()
    val viewModel: FilmMediaSwitcherViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.exposurePusher,
            container.filmMediumCompletionSender,
            container.filmMediaSyncRequestSender,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        ScreenScaffold { Text("Opening Exposures...") }
        return
    }

    val pages = remember(state.filmMedia) {
        if (state.filmMedia.isEmpty()) {
            listOf(SwitcherPage.Empty)
        } else {
            state.filmMedia.map { SwitcherPage.FilmMediumPage(it.id) } + SwitcherPage.Refresh
        }
    }
    val initialPage = remember(pages, state.initialFilmMediumId) {
        pages.indexOfFirst { it is SwitcherPage.FilmMediumPage && it.filmMediumId == state.initialFilmMediumId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }

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
                        SwitcherPageContent(
                            page = pages[page],
                            state = state,
                            viewModel = viewModel,
                            onFilmMediumSelected = onFilmMediumSelected,
                        )
                    }
                }
            }
        }
        PagerEdgeArrows(pagerState = pagerState)
    }

    val pendingFilmMedium = state.filmMedia.find { it.id == state.pendingCompleteFilmMediumId }
    AlertDialog(
        visible = pendingFilmMedium != null,
        onDismissRequest = viewModel::cancelCompleteFilmMedium,
        title = { Text("Complete this film?") },
        text = { pendingFilmMedium?.let { Text(it.name) } },
        confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = viewModel::confirmCompleteFilmMedium) },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = viewModel::cancelCompleteFilmMedium) },
    )
}

@Composable
private fun SwitcherPageContent(
    page: SwitcherPage,
    state: FilmMediaSwitcherUiState,
    viewModel: FilmMediaSwitcherViewModel,
    onFilmMediumSelected: (String) -> Unit,
) {
    when (page) {
        SwitcherPage.Empty ->
            CenteredPage {
                Text("No film yet - refresh or add on phone")
                RefreshSection(state, viewModel)
            }
        SwitcherPage.Refresh ->
            CenteredPage {
                RefreshSection(state, viewModel)
            }
        is SwitcherPage.FilmMediumPage -> {
            val filmMedium = state.filmMedia.find { it.id == page.filmMediumId } ?: return
            val isCompleted = filmMedium.status == FilmMediumStatus.COMPLETED
            CenteredPage {
                Text(filmMedium.name)
                Text("${filmMedium.filmStock} ${filmMedium.boxSpeedIso}")
                if (isCompleted) {
                    Text("Completed")
                }
                if (state.completeFilmMediumFailed) {
                    Text("Couldn't reach phone — try again")
                    Button(
                        label = { Text("Dismiss", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        onClick = viewModel::dismissCompleteFilmMediumFailure,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button(
                    label = { Text("Open", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    // Every non-completed film medium gets primary styling, not just the active one
                    // — see UX note #15 (exp-ux-notes-2026-08-21.md, Item 8). Deliberately gives up
                    // the "which one is active" visual signal this used to carry via button color;
                    // if that distinction turns out to matter, reintroduce it via a secondary cue
                    // (badge, border, label) rather than reverting this.
                    colors = when {
                        isCompleted -> ButtonDefaults.outlinedButtonColors()
                        else -> ButtonDefaults.buttonColors()
                    },
                    onClick = {
                        viewModel.selectFilmMedium(filmMedium.id)
                        onFilmMediumSelected(filmMedium.id)
                    },
                    onLongClick = { viewModel.requestCompleteFilmMedium(filmMedium.id) }.takeUnless { isCompleted },
                    onLongClickLabel = "Complete film",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RefreshSection(state: FilmMediaSwitcherUiState, viewModel: FilmMediaSwitcherViewModel) {
    Button(
        label = {
            Text(
                if (state.refreshInFlight) "Refreshing..." else "Refresh From Phone",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        enabled = !state.refreshInFlight,
        colors = ButtonDefaults.filledTonalButtonColors(),
        onClick = viewModel::refreshFromPhone,
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.refreshFailed) {
        Text("Couldn't reach phone")
        Button(
            label = { Text("Dismiss", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            colors = ButtonDefaults.filledTonalButtonColors(),
            onClick = viewModel::dismissRefreshFailure,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CenteredPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        content()
    }
}
