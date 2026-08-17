package com.exposures.watch.ui.exposureentry

import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.CameraBody
import com.exposures.model.Lens
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import com.exposures.model.Zone
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.watch.sync.CaptureRequestSender
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.FakeDataLayerGateway
import com.exposures.watch.sync.RollCompletionSender
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExposureEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gateway: FakeDataLayerGateway

    private suspend fun readyViewModel(
        repository: ExposureRepository? = null,
        rollId: String = DefaultSeedData.portra400Roll.id,
    ): ExposureEntryViewModel {
        val repo = repository ?: createSeededTestRepository()
        gateway = FakeDataLayerGateway()
        val viewModel = ExposureEntryViewModel(
            repo,
            ExposurePusher(repo, gateway),
            CaptureRequestSender(repo, gateway),
            RollCompletionSender(gateway),
            rollId,
        )
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }

    @Test
    fun `initial state loads the roll's camera body shutter speeds and box ISO`() = runTest {
        val state = readyViewModel().uiState.first { !it.isLoading }

        assertEquals(DefaultSeedData.rz67ProII.availableShutterSpeeds, state.availableShutterSpeeds)
        assertEquals(DefaultSeedData.portra400Roll.boxSpeedIso, state.iso)
        assertEquals(DefaultSeedData.lenses.toSet(), state.lenses.toSet())
    }

    @Test
    fun `initial state filters lenses to the active roll camera body`() = runTest {
        val repository = createSeededTestRepository()
        repository.applyCameraBodiesSync(
            DefaultSeedData.cameraBodies + CameraBody(
                id = "seed-body-rb67",
                name = "RB67 Pro S",
                manufacturer = "Mamiya",
                availableShutterSpeeds = listOf(ShutterSpeed.fraction(400)),
                hasBulbMode = true,
                createdAt = 0L,
                updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED,
                remoteId = null,
            ),
        )
        repository.applyLensesSync(
            DefaultSeedData.lenses + Lens(
                id = "seed-lens-180mm-rb67",
                name = "Mamiya Sekor C 180mm f/4.5",
                cameraBodyId = "seed-body-rb67",
                minAperture = 4.5,
                maxAperture = 32.0,
                stopIncrement = StopIncrement.HALF_STOP,
                referencePhotoZoomRatio = 1.0,
                createdAt = 0L,
                updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED,
                remoteId = null,
            ),
        )
        repository.applyFilmRollsSync(DefaultSeedData.filmRolls)

        val state = readyViewModel(repository, DefaultSeedData.portra400Roll.id).uiState.first { !it.isLoading }

        assertFalse(state.lenses.any { it.id == "seed-lens-180mm-rb67" })
    }

    @Test
    fun `a fresh entry defaults every picker to its first option, so it's submittable without touching anything`() = runTest {
        val state = readyViewModel().uiState.value

        assertTrue(state.canConfirm)
        assertEquals(state.lenses.first().id, state.selectedLensId)
        assertEquals(state.availableShutterSpeeds.first(), state.selectedShutterSpeed)
        assertEquals(state.availableApertures.first(), state.selectedAperture)
    }

    @Test
    fun `selecting a lens populates its available apertures`() = runTest {
        val viewModel = readyViewModel()

        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)

        assertEquals(DefaultSeedData.sekor110mmF28.availableApertures(), viewModel.uiState.value.availableApertures)
    }

    @Test
    fun `switching lenses resets aperture to the new lens's first available option`() = runTest {
        val viewModel = readyViewModel()
        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        viewModel.selectAperture(8.0)

        viewModel.selectLens(DefaultSeedData.sekor50mmF45.id)

        assertEquals(DefaultSeedData.sekor50mmF45.availableApertures().first(), viewModel.uiState.value.selectedAperture)
        assertEquals(DefaultSeedData.sekor50mmF45.availableApertures(), viewModel.uiState.value.availableApertures)
    }

    @Test
    fun `confirmSave persists the exposure with the chosen values and frame number 1`() = runTest {
        val viewModel = readyViewModel()
        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        viewModel.selectShutterSpeed(ShutterSpeed.fraction(250))
        viewModel.selectAperture(5.6)
        viewModel.setNotes("backlit, metered for shadows")

        viewModel.confirmSave()

        val state = viewModel.uiState.first { it.savedExposure != null }
        val saved = requireNotNull(state.savedExposure)
        assertEquals(1, saved.frameNumber)
        assertEquals(DefaultSeedData.sekor110mmF28.id, saved.lensId)
        assertEquals(ShutterSpeed.fraction(250), saved.shutterSpeed)
        assertEquals(5.6, saved.aperture, 0.0)
        assertEquals("backlit, metered for shadows", saved.notes)
    }

    @Test
    fun `confirmSave pushes the exposure list and sends a capture-photo command`() = runTest {
        val viewModel = readyViewModel()
        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        viewModel.selectShutterSpeed(ShutterSpeed.fraction(125))
        viewModel.selectAperture(8.0)

        viewModel.confirmSave()
        viewModel.uiState.first { it.savedExposure != null }

        assertTrue(gateway.putPayloads.isNotEmpty())
        assertEquals(1, gateway.sentMessages.size)
    }

    @Test
    fun `a fresh entry prefills the lens, shutter speed, and aperture last used on the same roll`() = runTest {
        val repository = createSeededTestRepository()
        val first = readyViewModel(repository)
        first.selectLens(DefaultSeedData.sekor50mmF45.id)
        first.selectShutterSpeed(ShutterSpeed.fraction(250))
        first.selectAperture(11.0)
        first.confirmSave()
        first.uiState.first { it.savedExposure != null }

        val second = readyViewModel(repository)

        assertEquals(DefaultSeedData.sekor50mmF45.id, second.uiState.value.selectedLensId)
        assertEquals(ShutterSpeed.fraction(250), second.uiState.value.selectedShutterSpeed)
        assertEquals(11.0, second.uiState.value.selectedAperture)
        assertEquals(DefaultSeedData.sekor50mmF45.availableApertures(), second.uiState.value.availableApertures)
    }

    @Test
    fun `last-used lens and aperture carry over to a different roll on the same camera body`() = runTest {
        val repository = createSeededTestRepository()
        val first = readyViewModel(repository, rollId = DefaultSeedData.portra400Roll.id)
        first.selectLens(DefaultSeedData.sekor110mmF28.id)
        first.selectShutterSpeed(ShutterSpeed.fraction(125))
        first.selectAperture(8.0)
        first.confirmSave()
        first.uiState.first { it.savedExposure != null }

        val second = readyViewModel(repository, rollId = DefaultSeedData.hp5Roll.id)

        assertEquals(DefaultSeedData.sekor110mmF28.id, second.uiState.value.selectedLensId)
        assertEquals(8.0, second.uiState.value.selectedAperture)
    }

    @Test
    fun `a roll with no light meter never shows the zone picker`() = runTest {
        val state = readyViewModel(rollId = DefaultSeedData.portra400Roll.id).uiState.value

        assertFalse(state.showZonePicker)
        assertNull(state.selectedZone)
    }

    @Test
    fun `a roll with a spot meter shows the zone picker defaulting to Zone VI`() = runTest {
        val state = readyViewModel(rollId = DefaultSeedData.hp5Roll.id).uiState.value

        assertTrue(state.showZonePicker)
        assertEquals(Zone.DEFAULT, state.selectedZone)
    }

    @Test
    fun `canConfirm is false when the zone picker is shown but no zone is selected`() {
        val state = ExposureEntryUiState(
            selectedLensId = "lens-1",
            selectedShutterSpeed = ShutterSpeed.fraction(125),
            selectedAperture = 8.0,
            showZonePicker = true,
            selectedZone = null,
        )

        assertFalse(state.canConfirm)
    }

    @Test
    fun `canConfirm is true once a zone is selected on a zone-picker roll`() {
        val state = ExposureEntryUiState(
            selectedLensId = "lens-1",
            selectedShutterSpeed = ShutterSpeed.fraction(125),
            selectedAperture = 8.0,
            showZonePicker = true,
            selectedZone = Zone.DEFAULT,
        )

        assertTrue(state.canConfirm)
    }

    @Test
    fun `canConfirm ignores the zone entirely when the picker isn't shown`() {
        val state = ExposureEntryUiState(
            selectedLensId = "lens-1",
            selectedShutterSpeed = ShutterSpeed.fraction(125),
            selectedAperture = 8.0,
            showZonePicker = false,
            selectedZone = null,
        )

        assertTrue(state.canConfirm)
    }

    @Test
    fun `selectZone updates the selected zone`() = runTest {
        val viewModel = readyViewModel(rollId = DefaultSeedData.hp5Roll.id)

        viewModel.selectZone(2)

        assertEquals(2, viewModel.uiState.value.selectedZone)
    }

    @Test
    fun `confirmSave persists the selected zone on a spot-metered roll`() = runTest {
        val viewModel = readyViewModel(rollId = DefaultSeedData.hp5Roll.id)
        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        viewModel.selectShutterSpeed(ShutterSpeed.fraction(125))
        viewModel.selectAperture(8.0)
        viewModel.selectZone(9)

        viewModel.confirmSave()

        val state = viewModel.uiState.first { it.savedExposure != null }
        assertEquals(9, requireNotNull(state.savedExposure).zone)
    }

    @Test
    fun `a saved exposure on a roll with no light meter has a null zone`() = runTest {
        val viewModel = readyViewModel(rollId = DefaultSeedData.portra400Roll.id)
        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        viewModel.selectShutterSpeed(ShutterSpeed.fraction(125))
        viewModel.selectAperture(8.0)

        viewModel.confirmSave()

        val state = viewModel.uiState.first { it.savedExposure != null }
        assertNull(requireNotNull(state.savedExposure).zone)
    }

    @Test
    fun `a chosen zone carries forward as the default the next time the picker is shown`() = runTest {
        val repository = createSeededTestRepository()
        val first = readyViewModel(repository, DefaultSeedData.hp5Roll.id)
        first.selectLens(DefaultSeedData.sekor110mmF28.id)
        first.selectShutterSpeed(ShutterSpeed.fraction(125))
        first.selectAperture(8.0)
        first.selectZone(1)
        first.confirmSave()
        first.uiState.first { it.savedExposure != null }

        val second = readyViewModel(repository, DefaultSeedData.hp5Roll.id)

        assertEquals(1, second.uiState.value.selectedZone)
    }

    private suspend fun readyViewModelOnLastFrame(): ExposureEntryViewModel {
        val repository = createSeededTestRepository()
        repository.applyFilmRollsSync(listOf(DefaultSeedData.portra400Roll.copy(targetFrameCount = 1)) + DefaultSeedData.filmRolls.drop(1))
        return readyViewModel(repository)
    }

    @Test
    fun `confirmSave on the roll's last frame also completes the roll on success`() = runTest {
        val viewModel = readyViewModelOnLastFrame()
        assertTrue(viewModel.uiState.value.isLastFrame)

        viewModel.confirmSave()
        val state = viewModel.uiState.first { it.rollCompleted || it.completeRollFailed }

        assertTrue(state.rollCompleted)
        assertNotNull(state.savedExposure)
        val (path, payload) = gateway.sentMessages.last()
        assertEquals(DataLayerPaths.COMPLETE_ROLL_COMMAND, path)
        assertEquals(DefaultSeedData.portra400Roll.id, DataLayerJson.decodeCompleteRollCommand(payload).rollId)
    }

    @Test
    fun `confirmSave on the last frame still saves the exposure even if completion fails`() = runTest {
        val viewModel = readyViewModelOnLastFrame()
        gateway.sendMessageResult = false

        viewModel.confirmSave()
        val state = viewModel.uiState.first { it.rollCompleted || it.completeRollFailed }

        assertTrue(state.completeRollFailed)
        assertFalse(state.rollCompleted)
        assertNotNull(state.savedExposure)
    }

    @Test
    fun `retryCompleteRoll resends the command without re-saving the exposure`() = runTest {
        val viewModel = readyViewModelOnLastFrame()
        gateway.sendMessageResult = false
        viewModel.confirmSave()
        viewModel.uiState.first { it.completeRollFailed }
        gateway.sendMessageResult = true

        viewModel.retryCompleteRoll()
        val state = viewModel.uiState.first { it.rollCompleted }

        assertTrue(state.rollCompleted)
    }

    @Test
    fun `dismissCompleteRollFailure clears the failure flag`() = runTest {
        val viewModel = readyViewModelOnLastFrame()
        gateway.sendMessageResult = false
        viewModel.confirmSave()
        viewModel.uiState.first { it.completeRollFailed }

        viewModel.dismissCompleteRollFailure()

        assertFalse(viewModel.uiState.value.completeRollFailed)
    }
}
