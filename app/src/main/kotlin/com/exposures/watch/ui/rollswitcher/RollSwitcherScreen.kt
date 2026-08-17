package com.exposures.watch.ui.rollswitcher

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
import com.exposures.model.RollStatus
import com.exposures.watch.ExposuresViewModelFactory
import com.exposures.watch.ui.appContainer
import com.exposures.watch.ui.components.PagerEdgeArrows

private sealed interface SwitcherPage {
    data class RollPage(val rollId: String) : SwitcherPage
    data object Refresh : SwitcherPage
    data object Empty : SwitcherPage
}

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

    if (state.isLoading) {
        ScreenScaffold { Text("Opening Exposures...") }
        return
    }

    val pages = remember(state.rolls) {
        if (state.rolls.isEmpty()) {
            listOf(SwitcherPage.Empty)
        } else {
            state.rolls.map { SwitcherPage.RollPage(it.id) } + SwitcherPage.Refresh
        }
    }
    val initialPage = remember(pages, state.initialRollId) {
        pages.indexOfFirst { it is SwitcherPage.RollPage && it.rollId == state.initialRollId }.coerceAtLeast(0)
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
                            onRollSelected = onRollSelected,
                        )
                    }
                }
            }
        }
        PagerEdgeArrows(pagerState = pagerState)
    }

    val pendingRoll = state.rolls.find { it.id == state.pendingCompleteRollId }
    AlertDialog(
        visible = pendingRoll != null,
        onDismissRequest = viewModel::cancelCompleteRoll,
        title = { Text("Complete this roll?") },
        text = { pendingRoll?.let { Text(it.name) } },
        confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = viewModel::confirmCompleteRoll) },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = viewModel::cancelCompleteRoll) },
    )
}

@Composable
private fun SwitcherPageContent(
    page: SwitcherPage,
    state: RollSwitcherUiState,
    viewModel: RollSwitcherViewModel,
    onRollSelected: (String) -> Unit,
) {
    when (page) {
        SwitcherPage.Empty ->
            CenteredPage {
                Text("No rolls yet - refresh or add on phone")
                RefreshSection(state, viewModel)
            }
        SwitcherPage.Refresh ->
            CenteredPage {
                RefreshSection(state, viewModel)
            }
        is SwitcherPage.RollPage -> {
            val roll = state.rolls.find { it.id == page.rollId } ?: return
            val isCompleted = roll.status == RollStatus.COMPLETED
            CenteredPage {
                Text(roll.name)
                Text("${roll.filmStock} ${roll.boxSpeedIso}")
                if (isCompleted) {
                    Text("Completed")
                }
                if (state.completeRollFailed) {
                    Text("Couldn't reach phone — try again")
                    Button(
                        label = { Text("Dismiss", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        onClick = viewModel::dismissCompleteRollFailure,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button(
                    label = { Text("Open", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    colors = when {
                        isCompleted -> ButtonDefaults.outlinedButtonColors()
                        roll.id == state.activeRollId -> ButtonDefaults.buttonColors()
                        else -> ButtonDefaults.filledTonalButtonColors()
                    },
                    onClick = {
                        viewModel.selectRoll(roll.id)
                        onRollSelected(roll.id)
                    },
                    onLongClick = { viewModel.requestCompleteRoll(roll.id) }.takeUnless { isCompleted },
                    onLongClickLabel = "Complete roll",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RefreshSection(state: RollSwitcherUiState, viewModel: RollSwitcherViewModel) {
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
