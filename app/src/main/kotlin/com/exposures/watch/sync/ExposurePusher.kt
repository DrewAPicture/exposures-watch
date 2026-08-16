package com.exposures.watch.sync

import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.mapper.toDto

/** Pushes the watch's full exposure list to the phone. Called after every save, not continuously observed. */
class ExposurePusher(
    private val repository: ExposureRepository,
    private val gateway: DataLayerGateway,
) {
    suspend fun push() {
        val exposures = repository.getAllExposuresOnce().map { it.toDto() }
        gateway.putPayload(DataLayerPaths.EXPOSURES, DataLayerJson.encodeExposures(exposures))
    }
}
