package com.exposures.watch.sync

import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.mapper.toDto
import com.exposures.watch.settings.OfflineModePreferences

/** Pushes the watch's full exposure list to the phone. Called after every save, not continuously observed. */
class ExposurePusher(
    private val repository: ExposureRepository,
    private val gateway: DataLayerGateway,
    private val offlineModePreferences: OfflineModePreferences,
    private val offlineActionQueue: OfflineActionQueue,
) {
    suspend fun push() {
        if (offlineModePreferences.isEnabledNow()) {
            offlineActionQueue.markExposurePushPending()
            return
        }
        val exposures = repository.getAllExposuresOnce().map { it.toDto() }
        gateway.putPayload(DataLayerPaths.EXPOSURES, DataLayerJson.encodeExposures(exposures))
        offlineActionQueue.clearPendingExposurePush()
    }

    suspend fun flushPending() {
        if (offlineModePreferences.isEnabledNow()) return
        if (!offlineActionQueue.hasPendingExposurePush()) return
        push()
    }
}
