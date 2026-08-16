package com.exposures.watch.sync

import com.exposures.database.seed.DefaultSeedData
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.dto.CaptureResultCommand
import com.exposures.datalayer.dto.PhotoStatusDto
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.watch.createSeededTestRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class PhotoStatusReceiverTest {

    private suspend fun savedExposure(repository: com.exposures.database.repository.ExposureRepository) =
        repository.saveExposure(
            Exposure(
                id = UUID.randomUUID().toString(), filmRollId = DefaultSeedData.portra400Roll.id, frameNumber = 0,
                lensId = DefaultSeedData.sekor110mmF28.id, shutterSpeed = ShutterSpeed.fraction(125), aperture = 8.0,
                isoUsed = 400, zone = null, notes = null, capturedAt = 0L, referencePhotoStatus = PhotoStatus.NONE,
                createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
            ),
        )

    @Test
    fun `handlePhotoStatusPayload updates matching exposures`() = runTest {
        val repository = createSeededTestRepository()
        val exposure = savedExposure(repository)
        val receiver = PhotoStatusReceiver(repository)
        val payload = DataLayerJson.encodePhotoStatuses(
            listOf(PhotoStatusDto(exposureId = exposure.id, referencePhotoStatus = "CAPTURED")),
        )

        receiver.handlePhotoStatusPayload(payload)

        assertEquals(PhotoStatus.CAPTURED, requireNotNull(repository.getExposure(exposure.id)).referencePhotoStatus)
    }

    @Test
    fun `handleCaptureResultMessage updates the exposure and clears any pending outbox entry`() = runTest {
        val repository = createSeededTestRepository()
        val exposure = savedExposure(repository)
        repository.enqueuePendingCaptureRequest(exposure.id, exposure.filmRollId, exposure.frameNumber)
        val receiver = PhotoStatusReceiver(repository)
        val payload = DataLayerJson.encodeCaptureResultCommand(CaptureResultCommand(exposure.id, "CAPTURED"))

        receiver.handleCaptureResultMessage(payload)

        assertEquals(PhotoStatus.CAPTURED, requireNotNull(repository.getExposure(exposure.id)).referencePhotoStatus)
        assertTrue(repository.getPendingCaptureRequests().isEmpty())
    }
}
