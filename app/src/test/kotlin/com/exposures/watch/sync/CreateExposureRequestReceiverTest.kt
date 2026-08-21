package com.exposures.watch.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.ExposuresDatabase
import com.exposures.database.repository.ExposureRepository
import com.exposures.database.seed.DefaultSeedData
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CreateExposureCommand
import com.exposures.datalayer.dto.ShutterSpeedDto
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.settings.OfflineModePreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class CreateExposureRequestReceiverTest {

    private fun createOfflineModePreferences(): OfflineModePreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_settings", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineModePreferences(context).also { it.setEnabled(false) }
    }

    private fun createOfflineActionQueue(): OfflineActionQueue {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_offline_queue", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineActionQueue(context)
    }

    private fun createReceiver(repository: ExposureRepository, gateway: FakeDataLayerGateway): CreateExposureRequestReceiver {
        val pusher = ExposurePusher(repository, gateway, createOfflineModePreferences(), createOfflineActionQueue())
        val sender = CaptureRequestSender(repository, gateway, createOfflineModePreferences())
        return CreateExposureRequestReceiver(repository, gateway, pusher, sender)
    }

    private fun ackFor(gateway: FakeDataLayerGateway) =
        DataLayerJson.decodeCreateExposureAckCommand(gateway.sentMessages.single { it.first == DataLayerPaths.CREATE_EXPOSURE_ACK_COMMAND }.second)

    @Test
    fun `accepted with all fields saves the exposure and acks acceptance`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway()
        val receiver = createReceiver(repository, gateway)
        val exposureId = UUID.randomUUID().toString()
        val command = CreateExposureCommand(
            exposureId = exposureId,
            shutterSpeed = ShutterSpeedDto("FRACTION", 1, 250),
            lensId = DefaultSeedData.sekor50mmF45.id,
            aperture = 5.6,
            isoUsed = 100,
            notes = "backlit",
        )

        receiver.handle(DataLayerJson.encodeCreateExposureCommand(command))

        val saved = requireNotNull(repository.getExposure(exposureId))
        assertEquals(DefaultSeedData.portra400Roll.id, saved.filmRollId)
        assertEquals(DefaultSeedData.sekor50mmF45.id, saved.lensId)
        assertEquals(DefaultSeedData.sekor50mmF45.focalLengthMm, saved.focalLengthMm)
        assertEquals(ShutterSpeed.fraction(250), saved.shutterSpeed)
        assertEquals(5.6, saved.aperture, 0.0)
        assertEquals(100, saved.isoUsed)
        assertEquals("backlit", saved.notes)
        assertEquals(PhotoStatus.NONE, saved.referencePhotoStatus)

        val ack = ackFor(gateway)
        assertEquals(exposureId, ack.exposureId)
        assertTrue(ack.accepted)
        assertNull(ack.reason)

        // also pushed the updated exposure list and sent a capture-photo request
        assertTrue(gateway.putPayloads.any { it.first == DataLayerPaths.EXPOSURES })
        assertTrue(gateway.sentMessages.any { it.first == DataLayerPaths.CAPTURE_PHOTO_COMMAND })
    }

    @Test
    fun `accepted with omitted fields defaults from last-used settings`() = runTest {
        val repository = createSeededTestRepository()
        repository.saveExposure(
            Exposure(
                id = UUID.randomUUID().toString(), filmRollId = DefaultSeedData.portra400Roll.id, frameNumber = 0,
                lensId = DefaultSeedData.sekor110mmF28.id, focalLengthMm = null, shutterSpeed = ShutterSpeed.fraction(125), aperture = 8.0,
                isoUsed = 400, zone = null, notes = null, capturedAt = 0L, referencePhotoStatus = PhotoStatus.NONE,
                createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
            ),
        )
        val gateway = FakeDataLayerGateway()
        val receiver = createReceiver(repository, gateway)
        val exposureId = UUID.randomUUID().toString()
        val command = CreateExposureCommand(exposureId = exposureId, shutterSpeed = ShutterSpeedDto("BULB", 1, 1))

        receiver.handle(DataLayerJson.encodeCreateExposureCommand(command))

        val saved = requireNotNull(repository.getExposure(exposureId))
        assertEquals(DefaultSeedData.sekor110mmF28.id, saved.lensId)
        assertEquals(8.0, saved.aperture, 0.0)
        assertEquals(400, saved.isoUsed)
        assertEquals(ShutterSpeed.BULB, saved.shutterSpeed)
        assertTrue(ackFor(gateway).accepted)
    }

    @Test
    fun `a replayed exposureId is a no-op`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway()
        val receiver = createReceiver(repository, gateway)
        val exposureId = UUID.randomUUID().toString()
        val command = CreateExposureCommand(
            exposureId = exposureId,
            shutterSpeed = ShutterSpeedDto("FRACTION", 1, 250),
            lensId = DefaultSeedData.sekor50mmF45.id,
            aperture = 5.6,
            isoUsed = 100,
        )
        receiver.handle(DataLayerJson.encodeCreateExposureCommand(command))
        gateway.sentMessages.clear()
        gateway.putPayloads.clear()

        receiver.handle(DataLayerJson.encodeCreateExposureCommand(command))

        assertTrue(gateway.sentMessages.isEmpty())
        assertTrue(gateway.putPayloads.isEmpty())
    }

    @Test
    fun `rejected when no active roll is selected`() = runTest {
        // A freshly-initialized repository (no seed data, no active roll set) matches how a real
        // fresh install reaches "no active roll" — equipment/rolls arrive from the phone via sync,
        // and the active roll is chosen locally afterward.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, ExposuresDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val freshRepository = ExposureRepository(database).also { it.ensureAppStateInitialized() }
        val gateway = FakeDataLayerGateway()
        val receiver = createReceiver(freshRepository, gateway)
        val exposureId = UUID.randomUUID().toString()
        val command = CreateExposureCommand(
            exposureId = exposureId,
            shutterSpeed = ShutterSpeedDto("FRACTION", 1, 250),
            lensId = "some-lens",
            aperture = 5.6,
            isoUsed = 100,
        )

        receiver.handle(DataLayerJson.encodeCreateExposureCommand(command))

        assertNull(freshRepository.getExposure(exposureId))
        val ack = ackFor(gateway)
        assertFalse(ack.accepted)
        assertEquals("No active roll selected on watch.", ack.reason)
    }

    @Test
    fun `rejected when no lens is specified and none was previously used`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway()
        val receiver = createReceiver(repository, gateway)
        val exposureId = UUID.randomUUID().toString()
        val command = CreateExposureCommand(exposureId = exposureId, shutterSpeed = ShutterSpeedDto("FRACTION", 1, 250))

        receiver.handle(DataLayerJson.encodeCreateExposureCommand(command))

        assertNull(repository.getExposure(exposureId))
        val ack = ackFor(gateway)
        assertFalse(ack.accepted)
        assertEquals("No lens specified and no previous lens to default to.", ack.reason)
    }
}
