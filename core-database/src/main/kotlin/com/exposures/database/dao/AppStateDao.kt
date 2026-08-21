package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.AppStateEntity
import com.exposures.model.ShutterSpeed
import kotlinx.coroutines.flow.Flow

/** Projection for the last-used exposure settings columns — see [AppStateDao.observeLastUsedExposureSettings]. */
data class LastUsedExposureSettingsRow(
    val lastLensId: String?,
    val lastShutterSpeed: ShutterSpeed?,
    val lastAperture: Double?,
    val lastIso: Int?,
    val lastZone: Int?,
    val lastFocalLengthMm: Int?,
)

@Dao
interface AppStateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureRowExists(row: AppStateEntity)

    @Query("SELECT activeRollId FROM app_state WHERE id = ${AppStateEntity.SINGLETON_ID}")
    fun observeActiveRollId(): Flow<String?>

    @Query("UPDATE app_state SET activeRollId = :rollId WHERE id = ${AppStateEntity.SINGLETON_ID}")
    suspend fun setActiveRollId(rollId: String?)

    @Query(
        "SELECT lastLensId, lastShutterSpeed, lastAperture, lastIso, lastZone, lastFocalLengthMm " +
            "FROM app_state WHERE id = ${AppStateEntity.SINGLETON_ID}",
    )
    fun observeLastUsedExposureSettings(): Flow<LastUsedExposureSettingsRow?>

    /**
     * [zone] and [focalLengthMm] use COALESCE rather than a straight assignment: [zone] is only
     * ever produced by a spot-metered roll, and [focalLengthMm] only by a ZOOM lens — saving an
     * exposure without one (PRIME lens, or a roll with no light meter) must not wipe out the last
     * real choice, unlike the other fields which are unconditionally required and so always have a
     * fresh value to write.
     */
    @Query(
        """
        UPDATE app_state SET lastLensId = :lensId, lastShutterSpeed = :shutterSpeed, lastAperture = :aperture, lastIso = :iso,
            lastZone = COALESCE(:zone, lastZone), lastFocalLengthMm = COALESCE(:focalLengthMm, lastFocalLengthMm)
        WHERE id = ${AppStateEntity.SINGLETON_ID}
        """,
    )
    suspend fun setLastUsedExposureSettings(
        lensId: String,
        shutterSpeed: ShutterSpeed,
        aperture: Double,
        iso: Int,
        zone: Int?,
        focalLengthMm: Int?,
    )
}
