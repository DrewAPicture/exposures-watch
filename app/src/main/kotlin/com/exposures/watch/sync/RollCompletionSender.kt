package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CompleteRollCommand

/**
 * Sends the complete-roll command to the phone. No outbox here (unlike [CaptureRequestSender]) —
 * per the plan, only capture requests get queued-and-retried; a failed completion just reports
 * failure so the UI can let the user retry the long-press.
 */
class RollCompletionSender(private val gateway: DataLayerGateway) {
    suspend fun complete(rollId: String): Boolean {
        val command = CompleteRollCommand(rollId)
        return gateway.sendMessage(DataLayerPaths.COMPLETE_ROLL_COMMAND, DataLayerJson.encodeCompleteRollCommand(command))
    }
}
