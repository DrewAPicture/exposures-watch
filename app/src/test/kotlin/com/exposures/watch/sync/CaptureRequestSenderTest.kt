package com.exposures.watch.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.settings.OfflineModePreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureRequestSenderTest {

    private fun createOfflineModePreferences(enabled: Boolean): OfflineModePreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_settings", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineModePreferences(context).also { it.setEnabled(enabled) }
    }

    @Test
    fun `a successful send does not queue anything in the outbox`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }
        val sender = CaptureRequestSender(repository, gateway, createOfflineModePreferences(enabled = false))

        sender.send("exp-1", "roll-1", 1)

        assertTrue(repository.getPendingCaptureRequests().isEmpty())
        val (path, payload) = gateway.sentMessages.single()
        assertEquals(DataLayerPaths.CAPTURE_PHOTO_COMMAND, path)
        assertEquals("exp-1", DataLayerJson.decodeCapturePhotoCommand(payload).exposureId)
    }

    @Test
    fun `an unreachable phone queues the request in the outbox`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = false }
        val sender = CaptureRequestSender(repository, gateway, createOfflineModePreferences(enabled = false))

        sender.send("exp-1", "roll-1", 1)

        val pending = repository.getPendingCaptureRequests()
        assertEquals(1, pending.size)
        assertEquals("exp-1", pending.single().exposureId)
    }

    @Test
    fun `flushPending retries every queued request and clears successful ones`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = false }
        val sender = CaptureRequestSender(repository, gateway, createOfflineModePreferences(enabled = false))
        sender.send("exp-1", "roll-1", 1)
        sender.send("exp-2", "roll-1", 2)
        gateway.sendMessageResult = true

        sender.flushPending()

        assertTrue(repository.getPendingCaptureRequests().isEmpty())
        assertEquals(4, gateway.sentMessages.size) // 2 failed attempts + 2 successful retries
    }

    @Test
    fun `flushPending leaves still-unreachable requests queued`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = false }
        val sender = CaptureRequestSender(repository, gateway, createOfflineModePreferences(enabled = false))
        sender.send("exp-1", "roll-1", 1)

        sender.flushPending()

        assertEquals(1, repository.getPendingCaptureRequests().size)
    }

    @Test
    fun `offline mode queues capture without attempting to message phone`() = runTest {
        val repository = createSeededTestRepository()
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }
        val sender = CaptureRequestSender(repository, gateway, createOfflineModePreferences(enabled = true))

        sender.send("exp-3", "roll-1", 3)

        assertTrue(gateway.sentMessages.isEmpty())
        assertEquals("exp-3", repository.getPendingCaptureRequests().single().exposureId)
    }
}
