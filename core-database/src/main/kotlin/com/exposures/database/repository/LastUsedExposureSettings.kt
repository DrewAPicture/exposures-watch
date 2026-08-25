package com.exposures.database.repository

import com.exposures.model.ShutterSpeed

/** Defaults for the next exposure entry, persisted globally (survives switching the active film medium). */
data class LastUsedExposureSettings(
    val lensId: String?,
    val shutterSpeed: ShutterSpeed?,
    val aperture: Double?,
    val iso: Int?,
    val exposureValue: Int?,
    val focalLengthMm: Int?,
)
