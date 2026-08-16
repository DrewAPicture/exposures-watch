package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.FilmFormat
import com.exposures.model.RollStatus
import com.exposures.model.SyncStatus

// Deliberately no FK to camera_bodies/light_meters: rolls and equipment arrive via independent
// Data Layer syncs from the phone with no cross-path ordering guarantee (same reasoning as the
// phone's exposure mirror), so a synced roll may reference equipment the watch hasn't received yet.
@Entity(
    tableName = "film_rolls",
    indices = [Index("cameraBodyId"), Index("lightMeterId")],
)
data class FilmRollEntity(
    @PrimaryKey val id: String,
    val name: String,
    val filmStock: String,
    val boxSpeedIso: Int,
    val format: FilmFormat,
    val cameraBodyId: String,
    val lightMeterId: String?,
    val targetFrameCount: Int,
    val status: RollStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
