package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A capture-photo request that couldn't be sent to the phone immediately (unreachable) and is
 * queued for retry once it becomes reachable again. One row per exposure — a later save
 * naturally supersedes an earlier unsent request for the same exposure.
 */
@Entity(tableName = "capture_request_outbox")
data class CaptureRequestOutboxEntity(
    @PrimaryKey val exposureId: String,
    val filmRollId: String,
    val frameNumber: Int,
    val createdAt: Long,
)
