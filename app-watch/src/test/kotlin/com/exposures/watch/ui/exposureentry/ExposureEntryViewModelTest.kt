package com.exposures.watch.ui.exposureentry

import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.ShutterSpeed
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.sync.CaptureRequestSender
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.FakeDataLayerGateway
import kotlinx.coroutines.flow.first
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
class ExposureEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gateway: FakeDataLayerGateway

    private suspend fun readyViewModel(): ExposureEntryViewModel {
        val repository = createSeededTestRepository()
        gateway = FakeDataLayerGateway()
        val viewModel = ExposureEntryViewModel(
            repository,
            ExposurePusher(repository, gateway),
            CaptureRequestSender(repository, gateway),
            DefaultSeedData.portra400Roll.id,
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
    fun `cannot confirm until lens, shutter speed, and aperture are all selected`() = runTest {
        val viewModel = readyViewModel()
        assertFalse(viewModel.uiState.value.canConfirm)

        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        assertFalse(viewModel.uiState.value.canConfirm)

        viewModel.selectShutterSpeed(ShutterSpeed.fraction(125))
        assertFalse(viewModel.uiState.value.canConfirm)

        viewModel.selectAperture(8.0)
        assertTrue(viewModel.uiState.value.canConfirm)
    }

    @Test
    fun `selecting a lens populates its available apertures`() = runTest {
        val viewModel = readyViewModel()

        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)

        assertEquals(DefaultSeedData.sekor110mmF28.availableApertures(), viewModel.uiState.value.availableApertures)
    }

    @Test
    fun `switching lenses clears a previously selected aperture`() = runTest {
        val viewModel = readyViewModel()
        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        viewModel.selectAperture(8.0)

        viewModel.selectLens(DefaultSeedData.sekor50mmF45.id)

        assertNull(viewModel.uiState.value.selectedAperture)
        assertEquals(DefaultSeedData.sekor50mmF45.availableApertures(), viewModel.uiState.value.availableApertures)
    }

    @Test
    fun `proceedToConfirm is a no-op until the picker selections are complete`() = runTest {
        val viewModel = readyViewModel()

        viewModel.proceedToConfirm()

        assertEquals(ExposureEntryStep.PICKERS, viewModel.uiState.value.step)
    }

    @Test
    fun `proceedToConfirm advances to the confirm step once selections are complete`() = runTest {
        val viewModel = readyViewModel()
        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        viewModel.selectShutterSpeed(ShutterSpeed.fraction(125))
        viewModel.selectAperture(8.0)

        viewModel.proceedToConfirm()

        assertEquals(ExposureEntryStep.CONFIRM, viewModel.uiState.value.step)
    }

    @Test
    fun `confirmSave persists the exposure with the chosen values and frame number 1`() = runTest {
        val viewModel = readyViewModel()
        viewModel.selectLens(DefaultSeedData.sekor110mmF28.id)
        viewModel.selectShutterSpeed(ShutterSpeed.fraction(250))
        viewModel.selectAperture(5.6)
        viewModel.setNotes("backlit, metered for shadows")
        viewModel.proceedToConfirm()

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
        viewModel.proceedToConfirm()

        viewModel.confirmSave()
        viewModel.uiState.first { it.savedExposure != null }

        assertTrue(gateway.putPayloads.isNotEmpty())
        assertEquals(1, gateway.sentMessages.size)
    }
}
