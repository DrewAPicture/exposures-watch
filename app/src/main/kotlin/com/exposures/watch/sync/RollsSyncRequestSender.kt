package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerPaths
import com.exposures.watch.settings.OfflineModePreferences

/** Requests a fresh phone snapshot now, or defers the request while Offline Mode is enabled. */
class RollsSyncRequestSender(
    private val gateway: DataLayerGateway,
    private val offlineModePreferences: OfflineModePreferences,
    private val offlineActionQueue: OfflineActionQueue,
) {
    suspend fun requestRefresh(): Boolean {
        if (offlineModePreferences.isEnabledNow()) {
            offlineActionQueue.markRefreshRequested()
            return true
        }
        return requestRefreshNow()
    }

    suspend fun flushPending() {
        if (offlineModePreferences.isEnabledNow()) return
        if (!offlineActionQueue.hasPendingRefresh()) return
        if (requestRefreshNow()) {
            offlineActionQueue.clearPendingRefresh()
        }
    }

    private suspend fun requestRefreshNow(): Boolean {
        val pingSent = gateway.sendMessage(DataLayerPaths.CONNECTIVITY_PING_COMMAND, "ping")
        if (!pingSent) return false
        return gateway.sendMessage(DataLayerPaths.REQUEST_ROLLS_SYNC_COMMAND, "refresh")
    }
}
