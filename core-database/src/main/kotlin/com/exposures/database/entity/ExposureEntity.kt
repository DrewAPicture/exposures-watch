package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus

@Entity(
    tableName = "exposures",
    foreignKeys = [
        ForeignKey(
            entity = FilmRollEntity::class,
            parentColumns = ["id"],
            childColumns = ["filmRollId"],
        ),
        ForeignKey(
            entity = LensEntity::class,
            parentColumns = ["id"],
            childColumns = ["lensId"],
        ),
    ],
    indices = [Index("filmRollId"), Index("lensId")],
)
data class ExposureEntity(
    @PrimaryKey val id: String,
    val filmRollId: String,
    val frameNumber: Int,
    val lensId: String,
    val shutterSpeed: ShutterSpeed,
    val aperture: Double,
    val isoUsed: Int,
    val notes: String?,
    val capturedAt: Long,
    val referencePhotoStatus: PhotoStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
