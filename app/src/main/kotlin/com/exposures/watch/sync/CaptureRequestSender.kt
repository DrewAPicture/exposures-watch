package com.exposures.watch.sync

import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CapturePhotoCommand
import com.exposures.watch.settings.OfflineModePreferences

/**
 * Sends the capture-photo command to the phone. If the phone is unreachable, queues the request
 * in the outbox instead of dropping it — [flushPending] retries everything queued there, meant to
 * be called from a reachability listener when the phone reconnects.
 */
class CaptureRequestSender(
    private val repository: ExposureRepository,
    private val gateway: DataLayerGateway,
    private val offlineModePreferences: OfflineModePreferences,
) {
    suspend fun send(exposureId: String, filmRollId: String, frameNumber: Int) {
        if (offlineModePreferences.isEnabledNow()) {
            repository.enqueuePendingCaptureRequest(exposureId, filmRollId, frameNumber)
            return
        }
        val command = CapturePhotoCommand(exposureId, filmRollId, frameNumber)
        val sent = gateway.sendMessage(DataLayerPaths.CAPTURE_PHOTO_COMMAND, DataLayerJson.encodeCapturePhotoCommand(command))
        if (sent) {
            repository.removePendingCaptureRequest(exposureId)
        } else {
            repository.enqueuePendingCaptureRequest(exposureId, filmRollId, frameNumber)
        }
    }

    suspend fun flushPending() {
        if (offlineModePreferences.isEnabledNow()) return
        repository.getPendingCaptureRequests().forEach { request ->
            send(request.exposureId, request.filmRollId, request.frameNumber)
        }
    }
}
