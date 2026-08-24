package com.exposures.watch.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.seed.DefaultSeedData
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.settings.OfflineModePreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ExposurePusherTest {

    private fun createOfflineModePreferences(enabled: Boolean): OfflineModePreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_settings", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineModePreferences(context).also { it.setEnabled(enabled) }
    }

    private fun createOfflineActionQueue(): OfflineActionQueue {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_offline_queue", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineActionQueue(context)
    }

    @Test
    fun `push writes the full exposure list to the exposures path`() = runTest {
        val repository = createSeededTestRepository()
        repository.saveExposure(
            Exposure(
                id = UUID.randomUUID().toString(), filmMediumId = DefaultSeedData.portra400Medium.id, frameNumber = 0,
                lensId = DefaultSeedData.sekor110mmF28.id, focalLengthMm = null, shutterSpeed = ShutterSpeed.fraction(125), aperture = 8.0,
                isoUsed = 400, zone = null, notes = null, capturedAt = 0L, referencePhotoStatus = PhotoStatus.NONE,
                createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
            ),
        )
        val gateway = FakeDataLayerGateway()
        val pusher = ExposurePusher(
            repository,
            gateway,
            createOfflineModePreferences(enabled = false),
            createOfflineActionQueue(),
        )

        pusher.push()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.EXPOSURES))
        assertEquals(1, DataLayerJson.decodeExposures(payload).size)
    }

    @Test
    fun `pushing with no exposures still writes an empty payload`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway()
        val pusher = ExposurePusher(
            repository,
            gateway,
            createOfflineModePreferences(enabled = false),
            createOfflineActionQueue(),
        )

        pusher.push()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.EXPOSURES))
        assertTrue(DataLayerJson.decodeExposures(payload).isEmpty())
    }

    @Test
    fun `offline mode defers exposure payload push`() = runTest {
        val repository = createSeededTestRepository()
        repository.saveExposure(
            Exposure(
                id = UUID.randomUUID().toString(), filmMediumId = DefaultSeedData.portra400Medium.id, frameNumber = 0,
                lensId = DefaultSeedData.sekor110mmF28.id, focalLengthMm = null, shutterSpeed = ShutterSpeed.fraction(125), aperture = 8.0,
                isoUsed = 400, zone = null, notes = null, capturedAt = 0L, referencePhotoStatus = PhotoStatus.NONE,
                createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
            ),
        )
        val gateway = FakeDataLayerGateway()
        val queue = createOfflineActionQueue()
        val pusher = ExposurePusher(
            repository,
            gateway,
            createOfflineModePreferences(enabled = true),
            queue,
        )

        pusher.push()

        assertTrue(gateway.putPayloads.isEmpty())
        assertTrue(queue.hasPendingExposurePush())
    }
}
