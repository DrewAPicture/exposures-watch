package com.exposures.watch.ui.rollswitcher

import com.exposures.database.seed.DefaultSeedData
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.sync.FakeDataLayerGateway
import com.exposures.watch.sync.RollsSyncRequestSender
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RollSwitcherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state lists the seeded rolls with the default active roll`() = runTest {
        val viewModel = RollSwitcherViewModel(
            createSeededTestRepository(),
            RollsSyncRequestSender(FakeDataLayerGateway()),
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(DefaultSeedData.filmRolls.toSet(), state.rolls.toSet())
        assertEquals(DefaultSeedData.filmRolls.first().id, state.activeRollId)
    }

    @Test
    fun `selecting a roll updates the active roll id`() = runTest {
        val viewModel = RollSwitcherViewModel(
            createSeededTestRepository(),
            RollsSyncRequestSender(FakeDataLayerGateway()),
        )
        viewModel.uiState.first { !it.isLoading }

        viewModel.selectRoll(DefaultSeedData.hp5Roll.id)

        val state = viewModel.uiState.first { it.activeRollId == DefaultSeedData.hp5Roll.id }
        assertEquals(DefaultSeedData.hp5Roll.id, state.activeRollId)
    }

    @Test
    fun `refreshFromPhone clears failure on success`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }
        val viewModel = RollSwitcherViewModel(
            createSeededTestRepository(),
            RollsSyncRequestSender(gateway),
        )

        viewModel.refreshFromPhone()
        advanceUntilIdle()

        val state = viewModel.uiState.first { !it.refreshInFlight }
        assertFalse(state.refreshFailed)
        assertEquals(
            listOf(
                com.exposures.datalayer.DataLayerPaths.CONNECTIVITY_PING_COMMAND,
                com.exposures.datalayer.DataLayerPaths.REQUEST_ROLLS_SYNC_COMMAND,
            ),
            gateway.sentMessages.map { it.first },
        )
    }

    @Test
    fun `refreshFromPhone shows failure when unreachable`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = false }
        val viewModel = RollSwitcherViewModel(
            createSeededTestRepository(),
            RollsSyncRequestSender(gateway),
        )

        viewModel.refreshFromPhone()
        advanceUntilIdle()

        val failedState = viewModel.uiState.first { it.refreshFailed }
        assertTrue(failedState.refreshFailed)
        viewModel.dismissRefreshFailure()
        val cleared = viewModel.uiState.first { !it.refreshFailed }
        assertFalse(cleared.refreshFailed)
    }
}
