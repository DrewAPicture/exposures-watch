package com.exposures.watch.ui.rolldetail

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RollDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state resolves the roll and its camera body`() = runTest {
        val repository = createSeededTestRepository()
        val viewModel = RollDetailViewModel(repository, DefaultSeedData.portra400Roll.id)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(DefaultSeedData.portra400Roll, state.roll)
        assertEquals(DefaultSeedData.rz67ProII, state.cameraBody)
        assertEquals(0, state.exposureCount)
        assertFalse(state.isComplete)
    }

    @Test
    fun `exposure count and completeness update as exposures are saved`() = runTest {
        val repository = createSeededTestRepository()
        val rollId = DefaultSeedData.portra400Roll.id // targetFrameCount = 10
        val viewModel = RollDetailViewModel(repository, rollId)
        viewModel.uiState.first { !it.isLoading }

        repeat(10) { frame ->
            repository.saveExposure(
                Exposure(
                    id = UUID.randomUUID().toString(),
                    filmRollId = rollId,
                    frameNumber = 0,
                    lensId = DefaultSeedData.sekor110mmF28.id,
                    shutterSpeed = ShutterSpeed.fraction(125),
                    aperture = 8.0,
                    isoUsed = 400,
                    zone = null,
                    notes = null,
                    capturedAt = frame.toLong(),
                    referencePhotoStatus = PhotoStatus.NONE,
                    createdAt = frame.toLong(),
                    updatedAt = frame.toLong(),
                    syncStatus = SyncStatus.PENDING_SYNC,
                    remoteId = null,
                ),
            )
        }

        val state = viewModel.uiState.first { it.exposureCount == 10 }
        assertTrue(state.isComplete)
    }
}
