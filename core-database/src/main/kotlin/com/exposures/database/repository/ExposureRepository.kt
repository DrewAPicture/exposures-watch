package com.exposures.database.repository

import com.exposures.database.ExposuresDatabase
import com.exposures.database.entity.AppStateEntity
import com.exposures.database.mapper.toDomain
import com.exposures.database.mapper.toEntity
import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmBack
import com.exposures.model.FilmMedium
import com.exposures.model.FilmMediumStatus
import com.exposures.model.Lens
import com.exposures.model.LightMeter
import com.exposures.model.PhotoStatus
import com.exposures.model.SyncStatus
import com.exposures.model.nextFrameNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The single data-access surface the watch app talks to: translates between Room entities and the
 * domain model.
 */
class ExposureRepository(private val database: ExposuresDatabase) {

    /** Populates [DefaultSeedData] if the relevant tables are empty — test-fixture use only, see its KDoc. */
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
        if (database.filmBackDao().count() == 0) {
            database.filmBackDao().upsertAll(DefaultSeedData.filmBacks.map { it.toEntity() })
        }
        if (database.filmMediumDao().count() == 0) {
            database.filmMediumDao().upsertAll(DefaultSeedData.filmMedia.map { it.toEntity() })
        }
        database.appStateDao().ensureRowExists(AppStateEntity(activeFilmMediumId = DefaultSeedData.filmMedia.first().id))
    }

    /**
     * Real app-startup bootstrap: creates the singleton `app_state` row if it doesn't exist yet,
     * with no active film medium, so [setActiveFilmMedium]/last-used-settings writes (both plain
     * `UPDATE`s) have a row to land on. Populates no equipment or film media — those arrive from
     * the phone via sync.
     */
    suspend fun ensureAppStateInitialized() {
        database.appStateDao().ensureRowExists(AppStateEntity(activeFilmMediumId = null))
    }

    /** The film medium the watch is currently recording exposures against. Switching is local to the watch. */
    fun observeActiveFilmMediumId(): Flow<String?> = database.appStateDao().observeActiveFilmMediumId()

    suspend fun setActiveFilmMedium(filmMediumId: String) = database.appStateDao().setActiveFilmMediumId(filmMediumId)

    fun observeCameraBodies(): Flow<List<CameraBody>> =
        database.cameraBodyDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getCameraBody(id: String): CameraBody? = database.cameraBodyDao().getById(id)?.toDomain()

    fun observeLenses(): Flow<List<Lens>> =
        database.lensDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getLens(id: String): Lens? = database.lensDao().getById(id)?.toDomain()

    fun observeLightMeters(): Flow<List<LightMeter>> =
        database.lightMeterDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getLightMeter(id: String): LightMeter? = database.lightMeterDao().getById(id)?.toDomain()

    fun observeFilmBacks(): Flow<List<FilmBack>> =
        database.filmBackDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getFilmBack(id: String): FilmBack? = database.filmBackDao().getById(id)?.toDomain()

    fun observeAvailableFilmMedia(): Flow<List<FilmMedium>> =
        database.filmMediumDao().getByStatus().map { entities -> entities.map { it.toDomain() } }

    /** AVAILABLE + COMPLETED film media for the switcher — see [com.exposures.database.dao.FilmMediumDao.getSwitcherFilmMedia]. */
    fun observeSwitcherFilmMedia(): Flow<List<FilmMedium>> =
        database.filmMediumDao().getSwitcherFilmMedia().map { entities -> entities.map { it.toDomain() } }

    /**
     * Marks a film medium COMPLETED locally, immediately — doesn't wait on the phone's authoritative
     * resync, so the switcher reflects completion right away regardless of whether notifying the
     * phone (see `FilmMediumCompletionSender`) succeeds.
     */
    suspend fun markFilmMediumCompletedLocally(filmMediumId: String) {
        database.filmMediumDao().updateStatus(filmMediumId, FilmMediumStatus.COMPLETED)
    }

    fun observeFilmMedium(id: String): Flow<FilmMedium?> =
        database.filmMediumDao().observeById(id).map { it?.toDomain() }

    suspend fun getFilmMedium(id: String): FilmMedium? = database.filmMediumDao().getById(id)?.toDomain()

    fun observeExposures(filmMediumId: String): Flow<List<Exposure>> =
        database.exposureDao().getByFilmMedium(filmMediumId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getExposure(id: String): Exposure? = database.exposureDao().getById(id)?.toDomain()

    /**
     * Persists an edit to an already-saved exposure. Frame number isn't editable here — the caller
     * keeps whatever [exposure.frameNumber] already had. Unlike [saveExposure], this never touches
     * the last-used-settings defaults: correcting a historical frame shouldn't change what a *new*
     * capture defaults to.
     */
    suspend fun updateExposure(exposure: Exposure) {
        database.exposureDao().upsert(
            exposure.copy(syncStatus = SyncStatus.PENDING_SYNC, updatedAt = System.currentTimeMillis()).toEntity(),
        )
    }

    /**
     * Persists [exposure], assigning it the next frame number for its film medium if one isn't
     * already set, and records its lens/shutter/aperture/ISO as the new last-used defaults (see
     * [observeLastUsedExposureSettings]).
     */
    suspend fun saveExposure(exposure: Exposure): Exposure {
        val resolved = if (exposure.frameNumber > 0) {
            exposure
        } else {
            val existing = database.exposureDao().getByFilmMedium(exposure.filmMediumId).first().map { it.toDomain() }
            exposure.copy(frameNumber = existing.nextFrameNumber())
        }
        database.exposureDao().upsert(resolved.toEntity())
        database.appStateDao().setLastUsedExposureSettings(
            lensId = resolved.lensId,
            shutterSpeed = resolved.shutterSpeed,
            aperture = resolved.aperture,
            iso = resolved.isoUsed,
            exposureValue = resolved.exposureValue,
            focalLengthMm = resolved.focalLengthMm,
        )
        return resolved
    }

    /** Defaults for the next exposure entry — see [saveExposure]. Persists across film medium switches. */
    fun observeLastUsedExposureSettings(): Flow<LastUsedExposureSettings> =
        database.appStateDao().observeLastUsedExposureSettings().map { row ->
            LastUsedExposureSettings(
                lensId = row?.lastLensId,
                shutterSpeed = row?.lastShutterSpeed,
                aperture = row?.lastAperture,
                iso = row?.lastIso,
                exposureValue = row?.lastExposureValue,
                focalLengthMm = row?.lastFocalLengthMm,
            )
        }

    /** Phone-driven merge: apply updates without destructive replacement on watch. */
    suspend fun applyCameraBodiesSync(bodies: List<CameraBody>) {
        database.cameraBodyDao().upsertAll(bodies.map { it.toEntity() })
    }

    /**
     * Phone-driven merge: apply lens updates without deleting historical rows. This keeps refresh
     * robust even when saved exposures still reference older lens ids.
     */
    suspend fun applyLensesSync(lenses: List<Lens>) {
        database.lensDao().upsertAll(lenses.map { it.toEntity() })
    }

    /** Phone-driven merge: apply meter updates without destructive replacement. */
    suspend fun applyLightMetersSync(lightMeters: List<LightMeter>) =
        database.lightMeterDao().upsertAll(lightMeters.map { it.toEntity() })

    /** Phone-driven merge: apply film back updates without destructive replacement. */
    suspend fun applyFilmBacksSync(filmBacks: List<FilmBack>) =
        database.filmBackDao().upsertAll(filmBacks.map { it.toEntity() })

    /**
     * Phone-driven merge for film medium updates. We only adjust the active film medium when the
     * payload explicitly marks the currently active one as not AVAILABLE.
     */
    suspend fun applyFilmMediaSync(filmMedia: List<FilmMedium>) {
        val activeFilmMediumId = database.appStateDao().observeActiveFilmMediumId().first()
        database.filmMediumDao().upsertAll(filmMedia.map { it.toEntity() })

        val incomingActiveFilmMedium = filmMedia.firstOrNull { it.id == activeFilmMediumId }
        if (incomingActiveFilmMedium != null && incomingActiveFilmMedium.status != FilmMediumStatus.AVAILABLE) {
            val fallback = database.filmMediumDao().getByStatus().first().firstOrNull()?.id
            database.appStateDao().setActiveFilmMediumId(fallback)
        }
    }

    /** Applied when the phone's photo-status sync or a capture-result ack arrives. */
    suspend fun updateExposurePhotoStatus(exposureId: String, status: PhotoStatus) {
        database.exposureDao().updatePhotoStatus(exposureId, status, System.currentTimeMillis())
    }

    /** Toggles the favorite flag on an existing frame. Does not touch last-used-settings defaults. */
    suspend fun toggleFavorite(exposureId: String, isFavorite: Boolean) {
        database.exposureDao().updateFavorite(exposureId, isFavorite, System.currentTimeMillis())
    }

    /** All exposures across all film media — used to build the payload pushed to the phone on every save. */
    suspend fun getAllExposuresOnce(): List<Exposure> = database.exposureDao().getAllOnce().map { it.toDomain() }
}
