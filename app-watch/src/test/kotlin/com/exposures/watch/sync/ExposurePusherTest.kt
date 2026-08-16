package com.exposures.watch.sync

import com.exposures.database.seed.DefaultSeedData
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
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
class ExposurePusherTest {

    @Test
    fun `push writes the full exposure list to the exposures path`() = runTest {
        val repository = createSeededTestRepository()
        repository.saveExposure(
            Exposure(
                id = UUID.randomUUID().toString(), filmRollId = DefaultSeedData.portra400Roll.id, frameNumber = 0,
                lensId = DefaultSeedData.sekor110mmF28.id, shutterSpeed = ShutterSpeed.fraction(125), aperture = 8.0,
                isoUsed = 400, zone = null, notes = null, capturedAt = 0L, referencePhotoStatus = PhotoStatus.NONE,
                createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
            ),
        )
        val gateway = FakeDataLayerGateway()
        val pusher = ExposurePusher(repository, gateway)

        pusher.push()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.EXPOSURES))
        assertEquals(1, DataLayerJson.decodeExposures(payload).size)
    }

    @Test
    fun `pushing with no exposures still writes an empty payload`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway()
        val pusher = ExposurePusher(repository, gateway)

        pusher.push()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.EXPOSURES))
        assertTrue(DataLayerJson.decodeExposures(payload).isEmpty())
    }
}
