package com.exposures.database.repository

import com.exposures.model.ShutterSpeed

/** Defaults for the next exposure entry, persisted globally (survives switching the active roll). */
data class LastUsedExposureSettings(
    val lensId: String?,
    val shutterSpeed: ShutterSpeed?,
    val aperture: Double?,
    val iso: Int?,
    val zone: Int?,
    val focalLengthMm: Int?,
)
