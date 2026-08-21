package com.exposures.watch.ui.framehistory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.repository.ExposureRepository
import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.settings.OfflineModePreferences
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.FakeDataLayerGateway
import com.exposures.watch.sync.OfflineActionQueue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class FrameHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun draft(rollId: String, frameNumber: Int) = Exposure(
        id = UUID.randomUUID().toString(),
        filmRollId = rollId,
        frameNumber = frameNumber,
        lensId = DefaultSeedData.sekor110mmF28.id,
        focalLengthMm = null,
        shutterSpeed = ShutterSpeed.fraction(125),
        aperture = 8.0,
        isoUsed = 400,
        zone = null,
        notes = null,
        capturedAt = frameNumber.toLong(),
        referencePhotoStatus = PhotoStatus.NONE,
        createdAt = frameNumber.toLong(),
        updatedAt = frameNumber.toLong(),
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    private fun testExposurePusher(repository: ExposureRepository, gateway: FakeDataLayerGateway): ExposurePusher {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("watch_offline_queue", Context.MODE_PRIVATE).edit().clear().commit()
        val offlineModePreferences = OfflineModePreferences(context).also { it.setEnabled(false) }
        val offlineActionQueue = OfflineActionQueue(context)
        return ExposurePusher(repository, gateway, offlineModePreferences, offlineActionQueue)
    }

    @Test
    fun `lists exposures for the roll most-recent-frame-first`() = runTest {
        val repository = createSeededTestRepository()
        val rollId = DefaultSeedData.portra400Roll.id
        repository.saveExposure(draft(rollId, 2))
        repository.saveExposure(draft(rollId, 1))
        // A frame logged against the other roll should never show up here.
        repository.saveExposure(draft(DefaultSeedData.hp5Roll.id, 1))

        val viewModel = FrameHistoryViewModel(repository, testExposurePusher(repository, FakeDataLayerGateway()), rollId)

        val state = viewModel.uiState.first { it.exposures.size == 2 }
        assertEquals(listOf(2, 1), state.exposures.map { it.frameNumber })
    }

    @Test
    fun `toggleFavorite flips the flag and is reflected in the next ui state`() = runTest {
        val repository = createSeededTestRepository()
        val rollId = DefaultSeedData.portra400Roll.id
        val saved = repository.saveExposure(draft(rollId, 1))
        val viewModel = FrameHistoryViewModel(repository, testExposurePusher(repository, FakeDataLayerGateway()), rollId)
        viewModel.uiState.first { it.exposures.isNotEmpty() }

        viewModel.toggleFavorite(saved.id, true)

        val favorited = viewModel.uiState.first { it.exposures.singleOrNull()?.isFavorite == true }
        assertEquals(true, favorited.exposures.single().isFavorite)
    }
}
