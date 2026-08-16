package com.exposures.watch.sync

import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.mapper.toDomain
import com.exposures.model.SyncStatus
import kotlinx.coroutines.flow.first

/** Applies incoming equipment/roll payloads from the phone to the local mirror. */
class EquipmentSyncReceiver(private val repository: ExposureRepository) {

    suspend fun handleCameraBodiesPayload(json: String) {
        val bodies = DataLayerJson.decodeCameraBodies(json).map { it.toDomain(syncStatus = SyncStatus.SYNCED) }
        val allowedBodyIds = bodies.map { it.id }.toSet()
        // Lenses now have a FK to camera bodies; when the body set is replaced, any lens still
        // pointing to a removed body would violate the constraint. Prune those first.
        val prunedLenses = repository.observeLenses().first()
            .filter { it.cameraBodyId == null || it.cameraBodyId in allowedBodyIds }
        repository.applyLensesSync(prunedLenses)
        repository.applyCameraBodiesSync(bodies)
    }

    suspend fun handleLensesPayload(json: String) {
        val lenses = DataLayerJson.decodeLenses(json).map { it.toDomain(syncStatus = SyncStatus.SYNCED) }
        repository.applyLensesSync(lenses)
    }

    suspend fun handleLightMetersPayload(json: String) {
        val lightMeters = DataLayerJson.decodeLightMeters(json).map { it.toDomain(syncStatus = SyncStatus.SYNCED) }
        repository.applyLightMetersSync(lightMeters)
    }

    suspend fun handleFilmRollsPayload(json: String) {
        val rolls = DataLayerJson.decodeRolls(json).map { it.toDomain(syncStatus = SyncStatus.SYNCED) }
        repository.applyFilmRollsSync(rolls)
    }
}
