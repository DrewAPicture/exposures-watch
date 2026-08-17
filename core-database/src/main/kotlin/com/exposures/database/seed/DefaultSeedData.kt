package com.exposures.database.seed

import com.exposures.model.CameraBody
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.LightMeter
import com.exposures.model.LightMeterType
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus

/**
 * Test-fixture data only — no longer wired into the real app (equipment/rolls now come from the
 * phone via sync; see `ExposureRepository.ensureAppStateInitialized`). Kept for `seedIfEmpty()`,
 * used by ViewModel/repository tests via `createSeededTestRepository()`. Fixed IDs and timestamp
 * keep it deterministic across test runs.
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
        cameraBodyId = rz67ProII.id,
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
        cameraBodyId = rz67ProII.id,
        minAperture = 4.5,
        maxAperture = 32.0,
        stopIncrement = StopIncrement.HALF_STOP,
        referencePhotoZoomRatio = 0.7, // wide lens — zoom out for a comparably wide reference photo
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    val pentaxSpotMeter = LightMeter(
        id = "seed-meter-pentax-spot",
        name = "Pentax Spotmeter V",
        manufacturer = "Pentax",
        type = LightMeterType.SPOT,
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    val rz67Back = FilmBack(
        id = "seed-back-rz67-6x7",
        name = "6x7 back",
        cameraBodyId = rz67ProII.id,
        type = FilmBackType.ROLL_6X7,
        availableFrameCounts = listOf(10),
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    // lightMeterId unset — exercises the no-Zone-picker path. This is the default roll used
    // throughout the existing test suite, so it deliberately stays meter-less to avoid every
    // unrelated test suddenly having to deal with a required zone picker.
    val portra400Roll = FilmRoll(
        id = "seed-roll-portra-400",
        name = "Portra 400 — Roll 1",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        colorType = FilmColorType.COLOR,
        cameraBodyId = rz67ProII.id,
        lightMeterId = null,
        targetFrameCount = 10,
        status = RollStatus.AVAILABLE,
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    // lightMeterId set — exercises the Zone picker.
    val hp5Roll = FilmRoll(
        id = "seed-roll-hp5-plus",
        name = "HP5 Plus — Roll 1",
        filmStock = "Ilford HP5 Plus",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        colorType = FilmColorType.BLACK_AND_WHITE,
        cameraBodyId = rz67ProII.id,
        lightMeterId = pentaxSpotMeter.id,
        targetFrameCount = 10,
        status = RollStatus.AVAILABLE,
        createdAt = SEED_TIMESTAMP,
        updatedAt = SEED_TIMESTAMP,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    val cameraBodies: List<CameraBody> = listOf(rz67ProII)
    val lenses: List<Lens> = listOf(sekor110mmF28, sekor50mmF45)
    val lightMeters: List<LightMeter> = listOf(pentaxSpotMeter)
    val filmBacks: List<FilmBack> = listOf(rz67Back)
    val filmRolls: List<FilmRoll> = listOf(portra400Roll, hp5Roll)
}
