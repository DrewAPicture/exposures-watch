package com.exposures.watch.sync

import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.mapper.toDomain
import com.exposures.model.SyncStatus

/** Applies incoming equipment/roll payloads from the phone to the local mirror. */
class EquipmentSyncReceiver(private val repository: ExposureRepository) {

    suspend fun handleCameraBodiesPayload(json: String) {
        val bodies = DataLayerJson.decodeCameraBodies(json).map { it.toDomain(syncStatus = SyncStatus.SYNCED) }
        repository.applyCameraBodiesSync(bodies)
    }

    suspend fun handleLensesPayload(json: String) {
        val lenses = DataLayerJson.decodeLenses(json).map { it.toDomain(syncStatus = SyncStatus.SYNCED) }
        repository.applyLensesSync(lenses)
    }

    suspend fun handleFilmRollsPayload(json: String) {
        val rolls = DataLayerJson.decodeRolls(json).map { it.toDomain(syncStatus = SyncStatus.SYNCED) }
        repository.applyFilmRollsSync(rolls)
    }
}
