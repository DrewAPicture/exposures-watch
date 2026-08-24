package com.exposures.watch.sync

import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.model.PhotoStatus

/** Applies the phone's photo-status sync (durable) or a capture-result ack (fast path) to the local mirror. */
class PhotoStatusReceiver(private val repository: ExposureRepository) {

    suspend fun handlePhotoStatusPayload(json: String) {
        DataLayerJson.decodePhotoStatuses(json).forEach { status ->
            repository.updateExposurePhotoStatus(status.exposureId, PhotoStatus.valueOf(status.referencePhotoStatus))
        }
    }

    suspend fun handleCaptureResultMessage(json: String) {
        val result = DataLayerJson.decodeCaptureResultCommand(json)
        repository.updateExposurePhotoStatus(result.exposureId, PhotoStatus.valueOf(result.status))
    }
}
