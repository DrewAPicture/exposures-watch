package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.dto.CameraBodyDto
import com.exposures.datalayer.dto.FilmRollDto
import com.exposures.datalayer.dto.LensDto
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
    fun `handleCameraBodiesPayload replaces the local camera body set as synced`() = runTest {
        val repository = createSeededTestRepository() // seeds one camera body
        val receiver = EquipmentSyncReceiver(repository)
        val body = CameraBodyDto(
            id = "phone-body-1", name = "RZ67 Pro II", manufacturer = "Mamiya",
            availableShutterSpeeds = emptyList(), hasBulbMode = true, createdAt = 0L, updatedAt = 0L,
        )

        receiver.handleCameraBodiesPayload(DataLayerJson.encodeCameraBodies(listOf(body)))

        val stored = repository.observeCameraBodies().first().single()
        assertEquals("phone-body-1", stored.id)
        assertEquals(SyncStatus.SYNCED, stored.syncStatus)
    }

    @Test
    fun `handleLensesPayload replaces the local lens set`() = runTest {
        val repository = createSeededTestRepository()
        val receiver = EquipmentSyncReceiver(repository)
        val lens = LensDto(
            id = "phone-lens-1", name = "110mm f/2.8 W", minAperture = 2.8, maxAperture = 32.0,
            stopIncrement = "HALF_STOP", referencePhotoZoomRatio = 1.0, createdAt = 0L, updatedAt = 0L,
        )

        receiver.handleLensesPayload(DataLayerJson.encodeLenses(listOf(lens)))

        assertEquals("phone-lens-1", repository.observeLenses().first().single().id)
    }

    @Test
    fun `handleFilmRollsPayload replaces the local roll set and repairs the active roll`() = runTest {
        val repository = createSeededTestRepository() // active roll is the seeded portra roll
        val receiver = EquipmentSyncReceiver(repository)
        val roll = FilmRollDto(
            id = "phone-roll-1", name = "New Roll", filmStock = "Ilford HP5", boxSpeedIso = 400,
            format = "MEDIUM_FORMAT_120", cameraBodyId = "phone-body-1", targetFrameCount = 10,
            status = "AVAILABLE", createdAt = 0L, updatedAt = 0L,
        )

        receiver.handleFilmRollsPayload(DataLayerJson.encodeRolls(listOf(roll)))

        assertEquals("phone-roll-1", repository.observeAvailableRolls().first().single().id)
        assertEquals("phone-roll-1", repository.observeActiveRollId().first())
    }
}
