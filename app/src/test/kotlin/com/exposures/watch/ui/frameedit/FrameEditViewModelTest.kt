package com.exposures.watch.ui.frameedit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.repository.ExposureRepository
import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.Exposure
import com.exposures.model.Lens
import com.exposures.model.LensType
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.FakeDataLayerGateway
import com.exposures.watch.sync.OfflineActionQueue
import com.exposures.watch.settings.OfflineModePreferences
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
class FrameEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun draftExposure(
        rollId: String,
        lensId: String = DefaultSeedData.sekor110mmF28.id,
        focalLengthMm: Int? = null,
        zone: Int? = null,
    ) = Exposure(
        id = UUID.randomUUID().toString(),
        filmRollId = rollId,
        frameNumber = 1,
        lensId = lensId,
        focalLengthMm = focalLengthMm,
        shutterSpeed = ShutterSpeed.fraction(125),
        aperture = 8.0,
        isoUsed = 400,
        zone = zone,
        notes = null,
        capturedAt = 0L,
        referencePhotoStatus = PhotoStatus.NONE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private suspend fun readyViewModel(repository: ExposureRepository, exposureId: String): FrameEditViewModel {
        val gateway = FakeDataLayerGateway()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("watch_offline_queue", Context.MODE_PRIVATE).edit().clear().commit()
        val offlineModePreferences = OfflineModePreferences(context).also { it.setEnabled(false) }
        val offlineActionQueue = OfflineActionQueue(context)
        val viewModel = FrameEditViewModel(
            repository,
            ExposurePusher(repository, gateway, offlineModePreferences, offlineActionQueue),
            exposureId,
        )
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }

    @Test
    fun `initial state prefills the draft with the exposure's current values`() = runTest {
        val repository = createSeededTestRepository()
        val exposure = repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))

        val state = readyViewModel(repository, exposure.id).uiState.first { !it.isLoading }

        assertEquals(exposure, state.draft)
        assertEquals(DefaultSeedData.lenses.toSet(), state.lenses.toSet())
        assertEquals(DefaultSeedData.rz67ProII.availableShutterSpeeds, state.availableShutterSpeeds)
        assertFalse(state.showZonePicker)
    }

    @Test
    fun `a roll with a spot meter requires a zone`() = runTest {
        val repository = createSeededTestRepository()
        val exposure = repository.saveExposure(draftExposure(DefaultSeedData.hp5Roll.id, zone = 5))

        val state = readyViewModel(repository, exposure.id).uiState.first { !it.isLoading }

        assertTrue(state.showZonePicker)
        assertTrue(state.canSave)
    }

    @Test
    fun `selecting a lens resets aperture to the new lens's first available option`() = runTest {
        val repository = createSeededTestRepository()
        val exposure = repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))
        val viewModel = readyViewModel(repository, exposure.id)

        viewModel.selectLens(DefaultSeedData.sekor50mmF45.id)

        val state = viewModel.uiState.first { it.draft?.lensId == DefaultSeedData.sekor50mmF45.id }
        assertEquals(DefaultSeedData.sekor50mmF45.availableApertures(), state.availableApertures)
        assertEquals(state.availableApertures.first(), state.draft?.aperture)
    }

    @Test
    fun `saveEdit persists the draft and marks the screen saved`() = runTest {
        val repository = createSeededTestRepository()
        val exposure = repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))
        val viewModel = readyViewModel(repository, exposure.id)

        viewModel.setIso(1600)
        viewModel.saveEdit()

        val state = viewModel.uiState.first { it.saved }
        assertEquals(1600, repository.getExposure(exposure.id)?.isoUsed)
        assertTrue(state.saved)
    }

    @Test
    fun `saveEdit does nothing when a required zone hasn't been chosen`() = runTest {
        val repository = createSeededTestRepository()
        val exposure = repository.saveExposure(draftExposure(DefaultSeedData.hp5Roll.id, zone = null))
        val viewModel = readyViewModel(repository, exposure.id)
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.saveEdit()

        assertFalse(viewModel.uiState.value.saved)
        assertEquals(SyncStatus.SYNCED, repository.getExposure(exposure.id)?.syncStatus)
    }

    private fun zoomLens(id: String = "seed-lens-zoom-24-70") = Lens(
        id = id,
        name = "24-70mm f/2.8",
        cameraBodyId = DefaultSeedData.rz67ProII.id,
        minAperture = 2.8,
        maxAperture = 22.0,
        stopIncrement = StopIncrement.THIRD_STOP,
        referencePhotoZoomRatio = 1.0,
        lensType = LensType.ZOOM,
        focalLengthMinMm = 24,
        focalLengthMaxMm = 70,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    @Test
    fun `switching to a zoom lens exposes the focal length page defaulting to its narrowest option`() = runTest {
        val repository = createSeededTestRepository()
        repository.applyLensesSync(DefaultSeedData.lenses + zoomLens())
        val exposure = repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))
        val viewModel = readyViewModel(repository, exposure.id)

        viewModel.selectLens("seed-lens-zoom-24-70")

        val state = viewModel.uiState.value
        assertTrue(state.showFocalLengthPicker)
        assertEquals((24..70).toList(), state.availableFocalLengths)
        assertEquals(24, state.draft?.focalLengthMm)
    }

    @Test
    fun `saveEdit does nothing when a zoom lens has no focal length chosen`() = runTest {
        val repository = createSeededTestRepository()
        repository.applyLensesSync(DefaultSeedData.lenses + zoomLens("seed-lens-zoom-empty").copy(focalLengthMinMm = null))
        val exposure = repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))
        val viewModel = readyViewModel(repository, exposure.id)
        viewModel.selectLens("seed-lens-zoom-empty")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.saveEdit()

        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `saveEdit persists a newly chosen zoom focal length`() = runTest {
        val repository = createSeededTestRepository()
        repository.applyLensesSync(DefaultSeedData.lenses + zoomLens())
        val exposure = repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))
        val viewModel = readyViewModel(repository, exposure.id)
        viewModel.selectLens("seed-lens-zoom-24-70")
        viewModel.selectFocalLength(50)

        viewModel.saveEdit()

        val state = viewModel.uiState.first { it.saved }
        assertTrue(state.saved)
        assertEquals(50, repository.getExposure(exposure.id)?.focalLengthMm)
    }
}
