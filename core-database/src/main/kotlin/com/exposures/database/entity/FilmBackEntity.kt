package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.FilmBackType
import com.exposures.model.SyncStatus

// Deliberately no FK to camera_bodies: equipment arrives via independent Data Layer syncs from the
// phone with no cross-path ordering guarantee (same reasoning as FilmMediumEntity), so a synced back
// may reference a camera body the watch hasn't received yet.
@Entity(
    tableName = "film_backs",
    indices = [Index("cameraBodyId")],
)
data class FilmBackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cameraBodyId: String,
    val type: FilmBackType,
    val availableFrameCounts: List<Int>,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
