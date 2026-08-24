package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.exposures.model.ShutterSpeed

/** Singleton row (always id = 0) holding watch-local state that isn't part of the synced domain model. */
@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val activeFilmMediumId: String?,
    // Last-used exposure settings, persisted globally (survives switching the active film medium) so the
    // next entry can default to them rather than starting blank every time.
    val lastLensId: String? = null,
    val lastShutterSpeed: ShutterSpeed? = null,
    val lastAperture: Double? = null,
    val lastIso: Int? = null,
    val lastZone: Int? = null,
    // Only ever produced by a ZOOM lens capture — see AppStateDao.setLastUsedExposureSettings's
    // COALESCE, same null-safety reasoning as lastZone.
    val lastFocalLengthMm: Int? = null,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
