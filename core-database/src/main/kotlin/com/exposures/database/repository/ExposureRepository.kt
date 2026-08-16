package com.exposures.database.repository

import com.exposures.database.ExposuresDatabase
import com.exposures.database.entity.AppStateEntity
import com.exposures.database.entity.CaptureRequestOutboxEntity
import com.exposures.database.mapper.toDomain
import com.exposures.database.mapper.toEntity
import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.LightMeter
import com.exposures.model.PhotoStatus
import com.exposures.model.RollStatus
import com.exposures.model.nextFrameNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The single data-access surface the watch app talks to: translates between Room entities and the
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
        if (database.lightMeterDao().count() == 0) {
            database.lightMeterDao().upsertAll(DefaultSeedData.lightMeters.map { it.toEntity() })
        }
        if (database.filmRollDao().count() == 0) {
            database.filmRollDao().upsertAll(DefaultSeedData.filmRolls.map { it.toEntity() })
        }
        database.appStateDao().ensureRowExists(AppStateEntity(activeRollId = DefaultSeedData.filmRolls.first().id))
    }

    /** The roll the watch is currently recording exposures against. Switching is local to the watch. */
    fun observeActiveRollId(): Flow<String?> = database.appStateDao().observeActiveRollId()

    suspend fun setActiveRoll(rollId: String) = database.appStateDao().setActiveRollId(rollId)

    fun observeCameraBodies(): Flow<List<CameraBody>> =
        database.cameraBodyDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getCameraBody(id: String): CameraBody? = database.cameraBodyDao().getById(id)?.toDomain()

    fun observeLenses(): Flow<List<Lens>> =
        database.lensDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getLens(id: String): Lens? = database.lensDao().getById(id)?.toDomain()

    fun observeLightMeters(): Flow<List<LightMeter>> =
        database.lightMeterDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getLightMeter(id: String): LightMeter? = database.lightMeterDao().getById(id)?.toDomain()

    fun observeAvailableRolls(): Flow<List<FilmRoll>> =
        database.filmRollDao().getByStatus().map { entities -> entities.map { it.toDomain() } }

    fun observeRoll(id: String): Flow<FilmRoll?> =
        database.filmRollDao().observeById(id).map { it?.toDomain() }

    suspend fun getRoll(id: String): FilmRoll? = database.filmRollDao().getById(id)?.toDomain()

    fun observeExposures(filmRollId: String): Flow<List<Exposure>> =
        database.exposureDao().getByRoll(filmRollId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getExposure(id: String): Exposure? = database.exposureDao().getById(id)?.toDomain()

    /**
     * Persists [exposure], assigning it the next frame number for its roll if one isn't already
     * set, and records its lens/shutter/aperture/ISO as the new last-used defaults (see
     * [observeLastUsedExposureSettings]).
     */
    suspend fun saveExposure(exposure: Exposure): Exposure {
        val resolved = if (exposure.frameNumber > 0) {
            exposure
        } else {
            val existing = database.exposureDao().getByRoll(exposure.filmRollId).first().map { it.toDomain() }
            exposure.copy(frameNumber = existing.nextFrameNumber())
        }
        database.exposureDao().upsert(resolved.toEntity())
        database.appStateDao().setLastUsedExposureSettings(
            lensId = resolved.lensId,
            shutterSpeed = resolved.shutterSpeed,
            aperture = resolved.aperture,
            iso = resolved.isoUsed,
            zone = resolved.zone,
        )
        return resolved
    }

    /** Defaults for the next exposure entry — see [saveExposure]. Persists across roll switches. */
    fun observeLastUsedExposureSettings(): Flow<LastUsedExposureSettings> =
        database.appStateDao().observeLastUsedExposureSettings().map { row ->
            LastUsedExposureSettings(
                lensId = row?.lastLensId,
                shutterSpeed = row?.lastShutterSpeed,
                aperture = row?.lastAperture,
                iso = row?.lastIso,
                zone = row?.lastZone,
            )
        }

    /** Phone-authoritative — full replace, matching what a fresh /equipment/camera-bodies sync sends. */
    suspend fun applyCameraBodiesSync(bodies: List<CameraBody>) {
        val bodyEntities = bodies.map { it.toEntity() }
        val dao = database.cameraBodyDao()
        dao.upsertAll(bodyEntities)
        if (bodyEntities.isEmpty()) {
            dao.deleteAll()
        } else {
            dao.deleteNotIn(bodyEntities.map { it.id })
        }
    }

    /** Phone-authoritative — full replace, matching what a fresh /equipment/lenses sync sends. */
    suspend fun applyLensesSync(lenses: List<Lens>) =
        database.lensDao().replaceAll(lenses.map { it.toEntity() })

    /** Phone-authoritative — full replace, matching what a fresh /equipment/light-meters sync sends. */
    suspend fun applyLightMetersSync(lightMeters: List<LightMeter>) =
        database.lightMeterDao().replaceAll(lightMeters.map { it.toEntity() })

    /**
     * Phone-authoritative — full replace. If the currently active roll no longer exists (e.g.
     * deleted on the phone) *or* is no longer AVAILABLE (e.g. just got marked COMPLETED — see
     * [RollCompletionSender][com.exposures.watch.sync.RollCompletionSender]), falls back to
     * another available roll, or clears the active roll entirely if none are left. A roll that's
     * merely completed isn't removed by this sync, so checking presence alone wouldn't catch it.
     */
    suspend fun applyFilmRollsSync(rolls: List<FilmRoll>) {
        database.filmRollDao().replaceAll(rolls.map { it.toEntity() })
        val activeRollId = database.appStateDao().observeActiveRollId().first()
        val activeRollStillAvailable = rolls.any { it.id == activeRollId && it.status == RollStatus.AVAILABLE }
        if (!activeRollStillAvailable) {
            val fallback = rolls.firstOrNull { it.status == RollStatus.AVAILABLE }?.id
            database.appStateDao().setActiveRollId(fallback)
        }
    }

    /** Applied when the phone's photo-status sync or a capture-result ack arrives. */
    suspend fun updateExposurePhotoStatus(exposureId: String, status: PhotoStatus) {
        database.exposureDao().updatePhotoStatus(exposureId, status, System.currentTimeMillis())
    }

    /** All exposures across all rolls — used to build the payload pushed to the phone on every save. */
    suspend fun getAllExposuresOnce(): List<Exposure> = database.exposureDao().getAllOnce().map { it.toDomain() }

    suspend fun enqueuePendingCaptureRequest(exposureId: String, filmRollId: String, frameNumber: Int) {
        database.captureRequestOutboxDao().enqueue(
            CaptureRequestOutboxEntity(exposureId, filmRollId, frameNumber, System.currentTimeMillis()),
        )
    }

    suspend fun removePendingCaptureRequest(exposureId: String) =
        database.captureRequestOutboxDao().remove(exposureId)

    suspend fun getPendingCaptureRequests(): List<PendingCaptureRequest> =
        database.captureRequestOutboxDao().getAll().map {
            PendingCaptureRequest(it.exposureId, it.filmRollId, it.frameNumber, it.createdAt)
        }
}
