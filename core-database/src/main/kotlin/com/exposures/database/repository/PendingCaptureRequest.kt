package com.exposures.database.repository

/** A capture-photo request queued because the phone was unreachable when it was first attempted. */
data class PendingCaptureRequest(
    val exposureId: String,
    val filmRollId: String,
    val frameNumber: Int,
    val createdAt: Long,
)
