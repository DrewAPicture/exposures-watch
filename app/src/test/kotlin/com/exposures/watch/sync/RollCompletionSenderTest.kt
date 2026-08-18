package com.exposures.watch.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.watch.settings.OfflineModePreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RollCompletionSenderTest {

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
    fun `complete sends a complete-roll command with the given roll id`() = runTest {
        val gateway = FakeDataLayerGateway()
        val sender = RollCompletionSender(
            gateway,
            createOfflineModePreferences(enabled = false),
            createOfflineActionQueue(),
        )

        sender.complete("roll-1")

        val (path, payload) = gateway.sentMessages.single()
        assertEquals(DataLayerPaths.COMPLETE_ROLL_COMMAND, path)
        assertEquals("roll-1", DataLayerJson.decodeCompleteRollCommand(payload).rollId)
    }

    @Test
    fun `complete returns true when the message was sent`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }

        assertTrue(
            RollCompletionSender(
                gateway,
                createOfflineModePreferences(enabled = false),
                createOfflineActionQueue(),
            ).complete("roll-1"),
        )
    }

    @Test
    fun `complete returns false when the phone is unreachable`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = false }

        assertFalse(
            RollCompletionSender(
                gateway,
                createOfflineModePreferences(enabled = false),
                createOfflineActionQueue(),
            ).complete("roll-1"),
        )
    }

    @Test
    fun `offline mode queues completion and reports success`() = runTest {
        val queue = createOfflineActionQueue()
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }
        val sender = RollCompletionSender(
            gateway,
            createOfflineModePreferences(enabled = true),
            queue,
        )

        val result = sender.complete("roll-1")

        assertTrue(result)
        assertTrue(gateway.sentMessages.isEmpty())
        assertTrue("roll-1" in queue.pendingRollCompletions())
    }
}
