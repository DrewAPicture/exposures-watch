package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CompleteRollCommand
import com.exposures.watch.settings.OfflineModePreferences

/** Sends the complete-roll command, or defers it while Offline Mode is enabled. */
class RollCompletionSender(
    private val gateway: DataLayerGateway,
    private val offlineModePreferences: OfflineModePreferences,
    private val offlineActionQueue: OfflineActionQueue,
) {
    suspend fun complete(rollId: String): Boolean {
        if (offlineModePreferences.isEnabledNow()) {
            offlineActionQueue.enqueueRollCompletion(rollId)
            return true
        }
        return sendNow(rollId)
    }

    suspend fun flushPending() {
        if (offlineModePreferences.isEnabledNow()) return
        offlineActionQueue.pendingRollCompletions().forEach { rollId ->
            if (sendNow(rollId)) {
                offlineActionQueue.removeRollCompletion(rollId)
            }
        }
    }

    private suspend fun sendNow(rollId: String): Boolean {
        val command = CompleteRollCommand(rollId)
        return gateway.sendMessage(DataLayerPaths.COMPLETE_ROLL_COMMAND, DataLayerJson.encodeCompleteRollCommand(command))
    }
}
