package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmMediumStatus
import com.exposures.model.FilmMediumType
import com.exposures.model.SyncStatus

// Deliberately no FK to camera_bodies/light_meters: film media and equipment arrive via independent
// Data Layer syncs from the phone with no cross-path ordering guarantee (same reasoning as the
// phone's exposure mirror), so a synced film medium may reference equipment the watch hasn't
// received yet.
@Entity(
    tableName = "film_media",
    indices = [Index("cameraBodyId"), Index("lightMeterId"), Index("filmBackId")],
)
data class FilmMediumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val filmStock: String,
    val boxSpeedIso: Int,
    val format: FilmFormat,
    val colorType: FilmColorType,
    val cameraBodyId: String,
    val lightMeterId: String?,
    /** Required for [FilmMediumType.ROLL]; unset for [FilmMediumType.SHEET]. */
    val filmBackId: String?,
    val type: FilmMediumType,
    val targetFrameCount: Int,
    val status: FilmMediumStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
