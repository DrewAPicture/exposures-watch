package com.exposures.watch.sync

import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CreateExposureAckCommand
import com.exposures.datalayer.mapper.toDomain
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.SyncStatus
import kotlinx.coroutines.flow.first

/**
 * Applies a phone-originated voice-capture create-exposure request. The watch stays authoritative
 * — this saves through the same [ExposureRepository.saveExposure] path manual entry uses (frame
 * numbering, last-used-settings defaulting) — then acks acceptance or rejection back to the phone.
 */
class CreateExposureRequestReceiver(
    private val repository: ExposureRepository,
    private val gateway: DataLayerGateway,
    private val exposurePusher: ExposurePusher,
    private val captureRequestSender: CaptureRequestSender,
) {
    suspend fun handle(json: String) {
        val command = DataLayerJson.decodeCreateExposureCommand(json)
        if (repository.getExposure(command.exposureId) != null) return // already applied — idempotent replay

        val rollId = repository.observeActiveRollId().first()
        if (rollId == null) {
            ack(command.exposureId, accepted = false, reason = "No active roll selected on watch.")
            return
        }

        val lastUsed = repository.observeLastUsedExposureSettings().first()
        val lensId = command.lensId ?: lastUsed.lensId
        if (lensId == null) {
            ack(command.exposureId, accepted = false, reason = "No lens specified and no previous lens to default to.")
            return
        }
        val aperture = command.aperture ?: lastUsed.aperture
        if (aperture == null) {
            ack(command.exposureId, accepted = false, reason = "No aperture specified and no previous aperture to default to.")
            return
        }
        val isoUsed = command.isoUsed ?: lastUsed.iso
        if (isoUsed == null) {
            ack(command.exposureId, accepted = false, reason = "No ISO specified and no previous ISO to default to.")
            return
        }

        val now = System.currentTimeMillis()
        val draft = Exposure(
            id = command.exposureId,
            filmRollId = rollId,
            frameNumber = 0, // resolved by saveExposure()
            lensId = lensId,
            shutterSpeed = command.shutterSpeed.toDomain(),
            aperture = aperture,
            isoUsed = isoUsed,
            zone = null,
            notes = command.notes,
            capturedAt = now,
            referencePhotoStatus = PhotoStatus.NONE,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC,
            remoteId = null,
        )
        val saved = repository.saveExposure(draft)
        exposurePusher.push()
        captureRequestSender.send(saved.id, saved.filmRollId, saved.frameNumber)
        ack(saved.id, accepted = true)
    }

    private suspend fun ack(exposureId: String, accepted: Boolean, reason: String? = null) {
        val ackCommand = CreateExposureAckCommand(exposureId, accepted, reason)
        gateway.sendMessage(DataLayerPaths.CREATE_EXPOSURE_ACK_COMMAND, DataLayerJson.encodeCreateExposureAckCommand(ackCommand))
    }
}
