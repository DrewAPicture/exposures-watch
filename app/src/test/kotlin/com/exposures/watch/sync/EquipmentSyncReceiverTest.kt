package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.dto.CameraBodyDto
import com.exposures.datalayer.dto.FilmBackDto
import com.exposures.datalayer.dto.FilmRollDto
import com.exposures.datalayer.dto.LensDto
import com.exposures.datalayer.dto.LightMeterDto
import com.exposures.model.SyncStatus
import com.exposures.watch.createSeededTestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EquipmentSyncReceiverTest {

    @Test
    fun `handleCameraBodiesPayload merges incoming bodies as synced`() = runTest {
        val repository = createSeededTestRepository() // seeds one camera body
        val receiver = EquipmentSyncReceiver(repository)
        val body = CameraBodyDto(
            id = "phone-body-1", name = "RZ67 Pro II", manufacturer = "Mamiya",
            availableShutterSpeeds = emptyList(), hasBulbMode = true, createdAt = 0L, updatedAt = 0L,
        )

        receiver.handleCameraBodiesPayload(DataLayerJson.encodeCameraBodies(listOf(body)))

        val stored = repository.observeCameraBodies().first().first { it.id == "phone-body-1" }
        assertEquals(SyncStatus.SYNCED, stored.syncStatus)
    }

    @Test
    fun `handleLensesPayload merges incoming lenses`() = runTest {
        val repository = createSeededTestRepository()
        val receiver = EquipmentSyncReceiver(repository)
        val lens = LensDto(
            id = "phone-lens-1", name = "110mm f/2.8 W", minAperture = 2.8, maxAperture = 32.0,
            stopIncrement = "HALF_STOP", referencePhotoZoomRatio = 1.0, createdAt = 0L, updatedAt = 0L,
        )

        receiver.handleLensesPayload(DataLayerJson.encodeLenses(listOf(lens)))

        assertEquals("phone-lens-1", repository.observeLenses().first().first { it.id == "phone-lens-1" }.id)
    }

    @Test
    fun `handleLightMetersPayload merges incoming light meters`() = runTest {
        val repository = createSeededTestRepository()
        val receiver = EquipmentSyncReceiver(repository)
        val meter = LightMeterDto(
            id = "phone-meter-1", name = "Spotmeter V", manufacturer = "Pentax",
            type = "SPOT", createdAt = 0L, updatedAt = 0L,
        )

        receiver.handleLightMetersPayload(DataLayerJson.encodeLightMeters(listOf(meter)))

        assertEquals(
            "phone-meter-1",
            repository.observeLightMeters().first().first { it.id == "phone-meter-1" }.id,
        )
    }

    @Test
    fun `handleFilmBacksPayload merges incoming film backs as synced`() = runTest {
        val repository = createSeededTestRepository()
        val receiver = EquipmentSyncReceiver(repository)
        val back = FilmBackDto(
            id = "phone-back-1", name = "6x7 back", cameraBodyId = "phone-body-1",
            type = "ROLL_6X7", availableFrameCounts = listOf(10, 11), createdAt = 0L, updatedAt = 0L,
        )

        receiver.handleFilmBacksPayload(DataLayerJson.encodeFilmBacks(listOf(back)))

        val stored = repository.observeFilmBacks().first().first { it.id == "phone-back-1" }
        assertEquals(SyncStatus.SYNCED, stored.syncStatus)
    }

    @Test
    fun `handleFilmRollsPayload merges incoming rolls`() = runTest {
        val repository = createSeededTestRepository()
        val receiver = EquipmentSyncReceiver(repository)
        val roll = FilmRollDto(
            id = "phone-roll-1", name = "New Roll", filmStock = "Ilford HP5", boxSpeedIso = 400,
            format = "MEDIUM_FORMAT_120", colorType = "COLOR", cameraBodyId = "phone-body-1", targetFrameCount = 10,
            status = "AVAILABLE", createdAt = 0L, updatedAt = 0L,
        )

        receiver.handleFilmRollsPayload(DataLayerJson.encodeRolls(listOf(roll)))

        assertEquals("phone-roll-1", repository.observeAvailableRolls().first().first { it.id == "phone-roll-1" }.id)
    }
}
