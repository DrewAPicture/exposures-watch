package com.exposures.database.seed

import com.exposures.model.CameraBody
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus

/**
 * Bootstrap data for Phase 1, standing in for the phone-configured equipment/roll setup that
 * arrives in Phase 2. Fixed IDs and timestamp keep this deterministic for tests and for "does the
 * app still show my roll after a restart" manual verification.
 */
object DefaultSeedData {

    private const val SEED_TIMESTAMP = 0L

    val rz67ProII = CameraBody(
        id = "seed-body-rz67-pro-ii",
        name = "RZ67 Pro II",
        manufacturer = "Mamiya",
        availableShutterSpeeds = ShutterSpeed.standardRange(
            fastest = ShutterSpeed.fraction(400),
            slowest = ShutterSpeed.wholeSeconds(8),
            includeBulb = true,
        ),
        hasBulbMode = true,
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    val sekor110mmF28 = Lens(
        id = "seed-lens-110mm-f2.8",
        name = "Mamiya-Sekor Z 110mm f/2.8 W",
        minAperture = 2.8,
        maxAperture = 32.0,
        stopIncrement = StopIncrement.HALF_STOP,
        referencePhotoZoomRatio = 1.0, // roughly a normal lens on 6x7 — no zoom needed
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    val sekor50mmF45 = Lens(
        id = "seed-lens-50mm-f4.5",
        name = "Mamiya-Sekor Z 50mm f/4.5 W",
        minAperture = 4.5,
        maxAperture = 32.0,
        stopIncrement = StopIncrement.HALF_STOP,
        referencePhotoZoomRatio = 0.7, // wide lens — zoom out for a comparably wide reference photo
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    val portra400Roll = FilmRoll(
        id = "seed-roll-portra-400",
        name = "Portra 400 — Roll 1",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        cameraBodyId = rz67ProII.id,
        targetFrameCount = 10,
        status = RollStatus.AVAILABLE,
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    val hp5Roll = FilmRoll(
        id = "seed-roll-hp5-plus",
        name = "HP5 Plus — Roll 1",
        filmStock = "Ilford HP5 Plus",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        cameraBodyId = rz67ProII.id,
        targetFrameCount = 10,
        status = RollStatus.AVAILABLE,
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    val cameraBodies: List<CameraBody> = listOf(rz67ProII)
    val lenses: List<Lens> = listOf(sekor110mmF28, sekor50mmF45)
    val filmRolls: List<FilmRoll> = listOf(portra400Roll, hp5Roll)
}
