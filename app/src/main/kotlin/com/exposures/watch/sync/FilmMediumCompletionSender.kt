package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CompleteFilmMediumCommand
import com.exposures.watch.settings.OfflineModePreferences

/** Sends the complete-film-medium command, or defers it while Offline Mode is enabled. */
class FilmMediumCompletionSender(
    private val gateway: DataLayerGateway,
    private val offlineModePreferences: OfflineModePreferences,
    private val offlineActionQueue: OfflineActionQueue,
) {
    suspend fun complete(filmMediumId: String): Boolean {
        if (offlineModePreferences.isEnabledNow()) {
            offlineActionQueue.enqueueFilmMediumCompletion(filmMediumId)
            return true
        }
        return sendNow(filmMediumId)
    }

    suspend fun flushPending() {
        if (offlineModePreferences.isEnabledNow()) return
        offlineActionQueue.pendingFilmMediumCompletions().forEach { filmMediumId ->
            if (sendNow(filmMediumId)) {
                offlineActionQueue.removeFilmMediumCompletion(filmMediumId)
            }
        }
    }

    private suspend fun sendNow(filmMediumId: String): Boolean {
        val command = CompleteFilmMediumCommand(filmMediumId)
        return gateway.sendMessage(DataLayerPaths.COMPLETE_FILM_MEDIUM_COMMAND, DataLayerJson.encodeCompleteFilmMediumCommand(command))
    }
}
