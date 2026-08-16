package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerPaths

/**
 * Explicitly asks the phone to re-push its authoritative film-roll list. This gives the watch a
 * deterministic manual recovery path if roll sync appears stale.
 */
class RollsSyncRequestSender(private val gateway: DataLayerGateway) {
    suspend fun requestRefresh(): Boolean {
        val pingSent = gateway.sendMessage(DataLayerPaths.CONNECTIVITY_PING_COMMAND, "ping")
        if (!pingSent) return false
        return gateway.sendMessage(DataLayerPaths.REQUEST_ROLLS_SYNC_COMMAND, "refresh")
    }
}
