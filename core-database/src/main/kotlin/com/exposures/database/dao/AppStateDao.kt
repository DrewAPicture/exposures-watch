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
)

@Dao
interface AppStateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureRowExists(row: AppStateEntity)

    @Query("SELECT activeRollId FROM app_state WHERE id = ${AppStateEntity.SINGLETON_ID}")
    fun observeActiveRollId(): Flow<String?>

    @Query("UPDATE app_state SET activeRollId = :rollId WHERE id = ${AppStateEntity.SINGLETON_ID}")
    suspend fun setActiveRollId(rollId: String?)

    @Query("SELECT lastLensId, lastShutterSpeed, lastAperture, lastIso, lastZone FROM app_state WHERE id = ${AppStateEntity.SINGLETON_ID}")
    fun observeLastUsedExposureSettings(): Flow<LastUsedExposureSettingsRow?>

    /**
     * [zone] uses COALESCE rather than a straight assignment: it's only ever produced by a
     * spot-metered roll, and saving an exposure on a roll without one (zone = null) must not wipe
     * out the last real zone choice, unlike the other fields which are unconditionally required
     * and so always have a fresh value to write.
     */
    @Query(
        """
        UPDATE app_state SET lastLensId = :lensId, lastShutterSpeed = :shutterSpeed, lastAperture = :aperture, lastIso = :iso,
            lastZone = COALESCE(:zone, lastZone)
        WHERE id = ${AppStateEntity.SINGLETON_ID}
        """,
    )
    suspend fun setLastUsedExposureSettings(lensId: String, shutterSpeed: ShutterSpeed, aperture: Double, iso: Int, zone: Int?)
}
