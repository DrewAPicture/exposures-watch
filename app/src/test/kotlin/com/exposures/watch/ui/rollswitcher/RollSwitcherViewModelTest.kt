package com.exposures.watch.ui.rollswitcher

import com.exposures.database.repository.ExposureRepository
import com.exposures.database.seed.DefaultSeedData
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.sync.FakeDataLayerGateway
import com.exposures.watch.sync.RollCompletionSender
import com.exposures.watch.sync.RollsSyncRequestSender
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RollSwitcherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gateway: FakeDataLayerGateway

    private suspend fun readyViewModel(repository: ExposureRepository? = null): RollSwitcherViewModel {
        val repo = repository ?: createSeededTestRepository()
        gateway = FakeDataLayerGateway()
        val viewModel = RollSwitcherViewModel(repo, RollsSyncRequestSender(gateway), RollCompletionSender(gateway))
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }

    @Test
    fun `initial state lists the seeded rolls with the default active roll`() = runTest {
        val state = readyViewModel().uiState.first { !it.isLoading }

        assertEquals(DefaultSeedData.filmRolls.toSet(), state.rolls.toSet())
        assertEquals(DefaultSeedData.filmRolls.first().id, state.activeRollId)
    }

    @Test
    fun `selecting a roll updates the active roll id`() = runTest {
        val viewModel = readyViewModel()

        viewModel.selectRoll(DefaultSeedData.hp5Roll.id)

        val state = viewModel.uiState.first { it.activeRollId == DefaultSeedData.hp5Roll.id }
        assertEquals(DefaultSeedData.hp5Roll.id, state.activeRollId)
    }

    @Test
    fun `initialRollId resolves to the active roll`() = runTest {
        val viewModel = readyViewModel()

        viewModel.selectRoll(DefaultSeedData.hp5Roll.id)

        val state = viewModel.uiState.first { it.activeRollId == DefaultSeedData.hp5Roll.id }
        assertEquals(DefaultSeedData.hp5Roll.id, state.initialRollId)
    }

    @Test
    fun `initialRollId falls back to the first roll when the active roll isn't among the listed rolls`() = runTest {
        val viewModel = readyViewModel()

        viewModel.selectRoll("not-a-real-roll-id")

        val state = viewModel.uiState.first { it.activeRollId == "not-a-real-roll-id" }
        assertEquals(state.rolls.first().id, state.initialRollId)
    }

    @Test
    fun `refreshFromPhone clears failure on success`() = runTest {
        val viewModel = readyViewModel()
        gateway.sendMessageResult = true

        viewModel.refreshFromPhone()
        advanceUntilIdle()

        val state = viewModel.uiState.first { !it.refreshInFlight }
        assertFalse(state.refreshFailed)
        assertEquals(
            listOf(DataLayerPaths.CONNECTIVITY_PING_COMMAND, DataLayerPaths.REQUEST_ROLLS_SYNC_COMMAND),
            gateway.sentMessages.map { it.first },
        )
    }

    @Test
    fun `refreshFromPhone shows failure when unreachable`() = runTest {
        val viewModel = readyViewModel()
        gateway.sendMessageResult = false

        viewModel.refreshFromPhone()
        advanceUntilIdle()

        val failedState = viewModel.uiState.first { it.refreshFailed }
        assertTrue(failedState.refreshFailed)
        viewModel.dismissRefreshFailure()
        val cleared = viewModel.uiState.first { !it.refreshFailed }
        assertFalse(cleared.refreshFailed)
    }

    @Test
    fun `requestCompleteRoll surfaces a pending roll without completing it`() = runTest {
        val viewModel = readyViewModel()

        viewModel.requestCompleteRoll(DefaultSeedData.hp5Roll.id)
        val state = viewModel.uiState.first { it.pendingCompleteRollId != null }

        assertEquals(DefaultSeedData.hp5Roll.id, state.pendingCompleteRollId)
        assertTrue(gateway.sentMessages.isEmpty())
    }

    @Test
    fun `cancelCompleteRoll dismisses the pending roll without sending anything`() = runTest {
        val viewModel = readyViewModel()
        viewModel.requestCompleteRoll(DefaultSeedData.hp5Roll.id)
        viewModel.uiState.first { it.pendingCompleteRollId != null }

        viewModel.cancelCompleteRoll()

        assertNull(viewModel.uiState.first { it.pendingCompleteRollId == null }.pendingCompleteRollId)
        assertTrue(gateway.sentMessages.isEmpty())
    }

    @Test
    fun `confirmCompleteRoll sends the command and clears the pending roll on success`() = runTest {
        val viewModel = readyViewModel()
        viewModel.requestCompleteRoll(DefaultSeedData.hp5Roll.id)
        viewModel.uiState.first { it.pendingCompleteRollId != null }

        viewModel.confirmCompleteRoll()
        val state = viewModel.uiState.first { it.pendingCompleteRollId == null && gateway.sentMessages.isNotEmpty() }

        assertFalse(state.completeRollFailed)
        val (path, payload) = gateway.sentMessages.single()
        assertEquals(DataLayerPaths.COMPLETE_ROLL_COMMAND, path)
        assertEquals(DefaultSeedData.hp5Roll.id, DataLayerJson.decodeCompleteRollCommand(payload).rollId)
    }

    @Test
    fun `confirmCompleteRoll surfaces failure when the phone is unreachable`() = runTest {
        val viewModel = readyViewModel()
        gateway.sendMessageResult = false
        viewModel.requestCompleteRoll(DefaultSeedData.hp5Roll.id)
        viewModel.uiState.first { it.pendingCompleteRollId != null }

        viewModel.confirmCompleteRoll()
        val failedState = viewModel.uiState.first { it.completeRollFailed }
        assertTrue(failedState.completeRollFailed)

        viewModel.dismissCompleteRollFailure()
        val cleared = viewModel.uiState.first { !it.completeRollFailed }
        assertFalse(cleared.completeRollFailed)
    }
}
