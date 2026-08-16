package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RollCompletionSenderTest {

    @Test
    fun `complete sends a complete-roll command with the given roll id`() = runTest {
        val gateway = FakeDataLayerGateway()
        val sender = RollCompletionSender(gateway)

        sender.complete("roll-1")

        val (path, payload) = gateway.sentMessages.single()
        assertEquals(DataLayerPaths.COMPLETE_ROLL_COMMAND, path)
        assertEquals("roll-1", DataLayerJson.decodeCompleteRollCommand(payload).rollId)
    }

    @Test
    fun `complete returns true when the message was sent`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = true }

        assertTrue(RollCompletionSender(gateway).complete("roll-1"))
    }

    @Test
    fun `complete returns false when the phone is unreachable`() = runTest {
        val gateway = FakeDataLayerGateway().apply { sendMessageResult = false }

        assertFalse(RollCompletionSender(gateway).complete("roll-1"))
    }
}
