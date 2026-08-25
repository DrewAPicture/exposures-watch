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
            entity = FilmMediumEntity::class,
            parentColumns = ["id"],
            childColumns = ["filmMediumId"],
        ),
        ForeignKey(
            entity = LensEntity::class,
            parentColumns = ["id"],
            childColumns = ["lensId"],
        ),
    ],
    indices = [Index("filmMediumId"), Index("lensId")],
)
data class ExposureEntity(
    @PrimaryKey val id: String,
    val filmMediumId: String,
    val frameNumber: Int,
    val lensId: String,
    val focalLengthMm: Int?,
    val shutterSpeed: ShutterSpeed,
    val aperture: Double,
    val isoUsed: Int,
    val exposureValue: Int?,
    val notes: String?,
    val capturedAt: Long,
    val referencePhotoStatus: PhotoStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
    val isFavorite: Boolean = false,
)
