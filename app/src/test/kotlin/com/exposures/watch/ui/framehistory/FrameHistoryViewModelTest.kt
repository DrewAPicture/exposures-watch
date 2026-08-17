package com.exposures.watch.ui.framehistory

import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
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

    @Test
    fun `lists exposures for the roll most-recent-frame-first`() = runTest {
        val repository = createSeededTestRepository()
        val rollId = DefaultSeedData.portra400Roll.id
        repository.saveExposure(draft(rollId, 2))
        repository.saveExposure(draft(rollId, 1))
        // A frame logged against the other roll should never show up here.
        repository.saveExposure(draft(DefaultSeedData.hp5Roll.id, 1))

        val viewModel = FrameHistoryViewModel(repository, rollId)

        val state = viewModel.uiState.first { it.exposures.size == 2 }
        assertEquals(listOf(2, 1), state.exposures.map { it.frameNumber })
    }
}
