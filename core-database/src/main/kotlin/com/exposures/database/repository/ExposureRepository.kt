package com.exposures.database.repository

import com.exposures.database.ExposuresDatabase
import com.exposures.database.mapper.toDomain
import com.exposures.database.mapper.toEntity
import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.nextFrameNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The single data-access surface app-watch talks to: translates between Room entities and the
 * domain model, and seeds bootstrap data on first launch (standing in for the phone-driven
 * equipment/roll sync that arrives in Phase 2).
 */
class ExposureRepository(private val database: ExposuresDatabase) {

    suspend fun seedIfEmpty() {
        if (database.cameraBodyDao().count() == 0) {
            database.cameraBodyDao().upsertAll(DefaultSeedData.cameraBodies.map { it.toEntity() })
        }
        if (database.lensDao().count() == 0) {
            database.lensDao().upsertAll(DefaultSeedData.lenses.map { it.toEntity() })
        }
        if (database.filmRollDao().count() == 0) {
            database.filmRollDao().upsertAll(DefaultSeedData.filmRolls.map { it.toEntity() })
        }
    }

    fun observeCameraBodies(): Flow<List<CameraBody>> =
        database.cameraBodyDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getCameraBody(id: String): CameraBody? = database.cameraBodyDao().getById(id)?.toDomain()

    fun observeLenses(): Flow<List<Lens>> =
        database.lensDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getLens(id: String): Lens? = database.lensDao().getById(id)?.toDomain()

    fun observeAvailableRolls(): Flow<List<FilmRoll>> =
        database.filmRollDao().getByStatus().map { entities -> entities.map { it.toDomain() } }

    fun observeRoll(id: String): Flow<FilmRoll?> =
        database.filmRollDao().observeById(id).map { it?.toDomain() }

    suspend fun getRoll(id: String): FilmRoll? = database.filmRollDao().getById(id)?.toDomain()

    fun observeExposures(filmRollId: String): Flow<List<Exposure>> =
        database.exposureDao().getByRoll(filmRollId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getExposure(id: String): Exposure? = database.exposureDao().getById(id)?.toDomain()

    /** Persists [exposure], assigning it the next frame number for its roll if one isn't already set. */
    suspend fun saveExposure(exposure: Exposure): Exposure {
        val resolved = if (exposure.frameNumber > 0) {
            exposure
        } else {
            val existing = database.exposureDao().getByRoll(exposure.filmRollId).first().map { it.toDomain() }
            exposure.copy(frameNumber = existing.nextFrameNumber())
        }
        database.exposureDao().upsert(resolved.toEntity())
        return resolved
    }
}
