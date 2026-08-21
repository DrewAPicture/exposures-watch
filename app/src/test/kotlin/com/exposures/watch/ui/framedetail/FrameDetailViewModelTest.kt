package com.exposures.watch.ui.framedetail

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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class FrameDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `resolves the exposure and its lens`() = runTest {
        val repository = createSeededTestRepository()
        val exposure = Exposure(
            id = UUID.randomUUID().toString(),
            filmRollId = DefaultSeedData.portra400Roll.id,
            frameNumber = 1,
            lensId = DefaultSeedData.sekor50mmF45.id,
            focalLengthMm = null,
            shutterSpeed = ShutterSpeed.fraction(60),
            aperture = 11.0,
            isoUsed = 400,
            zone = null,
            notes = "wide shot",
            capturedAt = 0L,
            referencePhotoStatus = PhotoStatus.NONE,
            createdAt = 0L,
            updatedAt = 0L,
            syncStatus = SyncStatus.PENDING_SYNC,
            remoteId = null,
        )
        repository.saveExposure(exposure)

        val viewModel = FrameDetailViewModel(repository, exposure.id)

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(exposure.copy(frameNumber = 1), state.exposure)
        assertEquals(DefaultSeedData.sekor50mmF45, state.lens)
    }

    @Test
    fun `unknown exposure id resolves to a null exposure`() = runTest {
        val viewModel = FrameDetailViewModel(createSeededTestRepository(), "does-not-exist")

        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.exposure)
    }
}
