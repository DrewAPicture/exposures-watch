package com.exposures.watch.ui.filmmediaswitcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.repository.ExposureRepository
import com.exposures.database.seed.DefaultSeedData
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.settings.OfflineModePreferences
import com.exposures.watch.sync.FakeDataLayerGateway
import com.exposures.model.FilmMediumStatus
import com.exposures.watch.sync.OfflineActionQueue
import com.exposures.watch.sync.FilmMediumCompletionSender
import com.exposures.watch.sync.FilmMediaSyncRequestSender
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
class FilmMediaSwitcherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gateway: FakeDataLayerGateway

    private fun createOfflineModePreferences(enabled: Boolean): OfflineModePreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_settings", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineModePreferences(context).also { it.setEnabled(enabled) }
    }

    private fun createOfflineActionQueue(): OfflineActionQueue {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_offline_queue", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineActionQueue(context)
    }

    private suspend fun readyViewModel(repository: ExposureRepository? = null): FilmMediaSwitcherViewModel {
        val repo = repository ?: createSeededTestRepository()
        gateway = FakeDataLayerGateway()
        val offlineModePreferences = createOfflineModePreferences(enabled = false)
        val offlineActionQueue = createOfflineActionQueue()
        val viewModel = FilmMediaSwitcherViewModel(
            repo,
            FilmMediaSyncRequestSender(gateway, offlineModePreferences, offlineActionQueue),
            FilmMediumCompletionSender(gateway, offlineModePreferences, offlineActionQueue),
        )
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }

    @Test
    fun `initial state lists the seeded film media with the default active film medium`() = runTest {
        val state = readyViewModel().uiState.first { !it.isLoading }

        assertEquals(DefaultSeedData.filmMedia.toSet(), state.filmMedia.toSet())
        assertEquals(DefaultSeedData.filmMedia.first().id, state.activeFilmMediumId)
    }

    @Test
    fun `selecting a film medium updates the active film medium id`() = runTest {
        val viewModel = readyViewModel()

        viewModel.selectFilmMedium(DefaultSeedData.hp5Medium.id)

        val state = viewModel.uiState.first { it.activeFilmMediumId == DefaultSeedData.hp5Medium.id }
        assertEquals(DefaultSeedData.hp5Medium.id, state.activeFilmMediumId)
    }

    @Test
    fun `initialFilmMediumId resolves to the active film medium`() = runTest {
        val viewModel = readyViewModel()

        viewModel.selectFilmMedium(DefaultSeedData.hp5Medium.id)

        val state = viewModel.uiState.first { it.activeFilmMediumId == DefaultSeedData.hp5Medium.id }
        assertEquals(DefaultSeedData.hp5Medium.id, state.initialFilmMediumId)
    }

    @Test
    fun `initialFilmMediumId falls back to the first film medium when the active one isn't among the listed film media`() = runTest {
        val viewModel = readyViewModel()

        viewModel.selectFilmMedium("not-a-real-medium-id")

        val state = viewModel.uiState.first { it.activeFilmMediumId == "not-a-real-medium-id" }
        assertEquals(state.filmMedia.first().id, state.initialFilmMediumId)
    }

    @Test
    fun `switcher lists completed film media alongside available ones`() = runTest {
        val repo = createSeededTestRepository()
        val viewModel = readyViewModel(repo)

        repo.markFilmMediumCompletedLocally(DefaultSeedData.hp5Medium.id)

        val state = viewModel.uiState.first { s ->
            s.filmMedia.any { it.id == DefaultSeedData.hp5Medium.id && it.status == FilmMediumStatus.COMPLETED }
        }
        assertTrue(state.filmMedia.any { it.id == DefaultSeedData.hp5Medium.id })
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
            listOf(DataLayerPaths.CONNECTIVITY_PING_COMMAND, DataLayerPaths.REQUEST_FILM_MEDIA_SYNC_COMMAND),
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
    fun `requestCompleteFilmMedium surfaces a pending film medium without completing it`() = runTest {
        val viewModel = readyViewModel()

        viewModel.requestCompleteFilmMedium(DefaultSeedData.hp5Medium.id)
        val state = viewModel.uiState.first { it.pendingCompleteFilmMediumId != null }

        assertEquals(DefaultSeedData.hp5Medium.id, state.pendingCompleteFilmMediumId)
        assertTrue(gateway.sentMessages.isEmpty())
    }

    @Test
    fun `cancelCompleteFilmMedium dismisses the pending film medium without sending anything`() = runTest {
        val viewModel = readyViewModel()
        viewModel.requestCompleteFilmMedium(DefaultSeedData.hp5Medium.id)
        viewModel.uiState.first { it.pendingCompleteFilmMediumId != null }

        viewModel.cancelCompleteFilmMedium()

        assertNull(viewModel.uiState.first { it.pendingCompleteFilmMediumId == null }.pendingCompleteFilmMediumId)
        assertTrue(gateway.sentMessages.isEmpty())
    }

    @Test
    fun `confirmCompleteFilmMedium sends the command and clears the pending film medium on success`() = runTest {
        val viewModel = readyViewModel()
        viewModel.requestCompleteFilmMedium(DefaultSeedData.hp5Medium.id)
        viewModel.uiState.first { it.pendingCompleteFilmMediumId != null }

        viewModel.confirmCompleteFilmMedium()
        val state = viewModel.uiState.first { it.pendingCompleteFilmMediumId == null && gateway.sentMessages.isNotEmpty() }

        assertFalse(state.completeFilmMediumFailed)
        val (path, payload) = gateway.sentMessages.single()
        assertEquals(DataLayerPaths.COMPLETE_FILM_MEDIUM_COMMAND, path)
        assertEquals(DefaultSeedData.hp5Medium.id, DataLayerJson.decodeCompleteFilmMediumCommand(payload).filmMediumId)
    }

    @Test
    fun `confirmCompleteFilmMedium surfaces failure when the phone is unreachable`() = runTest {
        val viewModel = readyViewModel()
        gateway.sendMessageResult = false
        viewModel.requestCompleteFilmMedium(DefaultSeedData.hp5Medium.id)
        viewModel.uiState.first { it.pendingCompleteFilmMediumId != null }

        viewModel.confirmCompleteFilmMedium()
        val failedState = viewModel.uiState.first { it.completeFilmMediumFailed }
        assertTrue(failedState.completeFilmMediumFailed)

        viewModel.dismissCompleteFilmMediumFailure()
        val cleared = viewModel.uiState.first { !it.completeFilmMediumFailed }
        assertFalse(cleared.completeFilmMediumFailed)
    }

    @Test
    fun `confirmCompleteFilmMedium marks the film medium completed locally on success`() = runTest {
        val repo = createSeededTestRepository()
        val viewModel = readyViewModel(repo)
        viewModel.requestCompleteFilmMedium(DefaultSeedData.hp5Medium.id)
        viewModel.uiState.first { it.pendingCompleteFilmMediumId != null }

        viewModel.confirmCompleteFilmMedium()
        viewModel.uiState.first { it.pendingCompleteFilmMediumId == null && gateway.sentMessages.isNotEmpty() }

        assertEquals(FilmMediumStatus.COMPLETED, repo.getFilmMedium(DefaultSeedData.hp5Medium.id)?.status)
    }

    @Test
    fun `confirmCompleteFilmMedium marks the film medium completed locally even when notifying the phone fails`() = runTest {
        val repo = createSeededTestRepository()
        val viewModel = readyViewModel(repo)
        gateway.sendMessageResult = false
        viewModel.requestCompleteFilmMedium(DefaultSeedData.hp5Medium.id)
        viewModel.uiState.first { it.pendingCompleteFilmMediumId != null }

        viewModel.confirmCompleteFilmMedium()
        viewModel.uiState.first { it.completeFilmMediumFailed }

        assertEquals(FilmMediumStatus.COMPLETED, repo.getFilmMedium(DefaultSeedData.hp5Medium.id)?.status)
    }
}
