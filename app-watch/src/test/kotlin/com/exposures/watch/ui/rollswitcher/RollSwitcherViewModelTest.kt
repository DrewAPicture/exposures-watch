package com.exposures.watch.ui.rollswitcher

import com.exposures.database.seed.DefaultSeedData
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        val viewModel = RollSwitcherViewModel(createSeededTestRepository())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(DefaultSeedData.filmRolls.toSet(), state.rolls.toSet())
        assertEquals(DefaultSeedData.filmRolls.first().id, state.activeRollId)
    }

    @Test
    fun `selecting a roll updates the active roll id`() = runTest {
        val viewModel = RollSwitcherViewModel(createSeededTestRepository())
        viewModel.uiState.first { !it.isLoading }

        viewModel.selectRoll(DefaultSeedData.hp5Roll.id)

        val state = viewModel.uiState.first { it.activeRollId == DefaultSeedData.hp5Roll.id }
        assertEquals(DefaultSeedData.hp5Roll.id, state.activeRollId)
    }
}
