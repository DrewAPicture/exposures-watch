package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerPaths
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RollsSyncRequestSenderTest {

    @Test
    fun `requestRefresh sends ping then refresh command when reachable`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }
        val sender = RollsSyncRequestSender(gateway)

        val result = sender.requestRefresh()

        assertTrue(result)
        assertEquals(
            listOf(
                DataLayerPaths.CONNECTIVITY_PING_COMMAND,
                DataLayerPaths.REQUEST_ROLLS_SYNC_COMMAND,
            ),
            gateway.sentMessages.map { it.first },
        )
    }

    @Test
    fun `requestRefresh returns false and skips refresh when ping fails`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = false }
        val sender = RollsSyncRequestSender(gateway)

        val result = sender.requestRefresh()

        assertFalse(result)
        assertEquals(listOf(DataLayerPaths.CONNECTIVITY_PING_COMMAND), gateway.sentMessages.map { it.first })
    }
}
