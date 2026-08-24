package com.exposures.watch.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class FilmMediaSyncRequestSenderTest {

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
    fun `requestRefresh sends ping then refresh command when reachable`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }
        val sender = FilmMediaSyncRequestSender(
            gateway,
            createOfflineModePreferences(enabled = false),
            createOfflineActionQueue(),
        )

        val result = sender.requestRefresh()

        assertTrue(result)
        assertEquals(
            listOf(
                DataLayerPaths.CONNECTIVITY_PING_COMMAND,
                DataLayerPaths.REQUEST_FILM_MEDIA_SYNC_COMMAND,
            ),
            gateway.sentMessages.map { it.first },
        )
    }

    @Test
    fun `requestRefresh returns false and skips refresh when ping fails`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = false }
        val sender = FilmMediaSyncRequestSender(
            gateway,
            createOfflineModePreferences(enabled = false),
            createOfflineActionQueue(),
        )

        val result = sender.requestRefresh()

        assertFalse(result)
        assertEquals(listOf(DataLayerPaths.CONNECTIVITY_PING_COMMAND), gateway.sentMessages.map { it.first })
    }

    @Test
    fun `offline mode defers refresh request without messaging phone`() = runTest {
        val queue = createOfflineActionQueue()
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }
        val sender = FilmMediaSyncRequestSender(
            gateway,
            createOfflineModePreferences(enabled = true),
            queue,
        )

        val result = sender.requestRefresh()

        assertTrue(result)
        assertTrue(queue.hasPendingRefresh())
        assertTrue(gateway.sentMessages.isEmpty())
    }
}
