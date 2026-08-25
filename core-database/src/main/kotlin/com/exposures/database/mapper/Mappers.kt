@file:JvmName("AppMappersKt")

package com.exposures.database.mapper

import com.exposures.database.entity.ExposureEntity
import com.exposures.database.entity.FilmBackEntity
import com.exposures.database.entity.FilmMediumEntity
import com.exposures.model.Exposure
import com.exposures.model.FilmBack
import com.exposures.model.FilmMedium

fun FilmBackEntity.toDomain() = FilmBack(
    id = id,
    name = name,
    cameraBodyId = cameraBodyId,
    type = type,
    availableFrameCounts = availableFrameCounts,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun FilmBack.toEntity() = FilmBackEntity(
    id = id,
    name = name,
    cameraBodyId = cameraBodyId,
    type = type,
    availableFrameCounts = availableFrameCounts,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun FilmMediumEntity.toDomain() = FilmMedium(
    id = id,
    name = name,
    filmStock = filmStock,
    boxSpeedIso = boxSpeedIso,
    format = format,
    colorType = colorType,
    cameraBodyId = cameraBodyId,
    lightMeterId = lightMeterId,
    filmBackId = filmBackId,
    type = type,
    targetFrameCount = targetFrameCount,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun FilmMedium.toEntity() = FilmMediumEntity(
    id = id,
    name = name,
    filmStock = filmStock,
    boxSpeedIso = boxSpeedIso,
    format = format,
    colorType = colorType,
    cameraBodyId = cameraBodyId,
    lightMeterId = lightMeterId,
    filmBackId = filmBackId,
    type = type,
    targetFrameCount = targetFrameCount,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun ExposureEntity.toDomain() = Exposure(
    id = id,
    filmMediumId = filmMediumId,
    frameNumber = frameNumber,
    lensId = lensId,
    focalLengthMm = focalLengthMm,
    shutterSpeed = shutterSpeed,
    aperture = aperture,
    isoUsed = isoUsed,
    exposureValue = exposureValue,
    notes = notes,
    capturedAt = capturedAt,
    referencePhotoStatus = referencePhotoStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
    isFavorite = isFavorite,
)

fun Exposure.toEntity() = ExposureEntity(
    id = id,
    filmMediumId = filmMediumId,
    frameNumber = frameNumber,
    lensId = lensId,
    focalLengthMm = focalLengthMm,
    shutterSpeed = shutterSpeed,
    aperture = aperture,
    isoUsed = isoUsed,
    exposureValue = exposureValue,
    notes = notes,
    capturedAt = capturedAt,
    referencePhotoStatus = referencePhotoStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
    isFavorite = isFavorite,
)
