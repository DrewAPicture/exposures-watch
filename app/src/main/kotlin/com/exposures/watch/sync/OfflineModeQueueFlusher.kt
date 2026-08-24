package com.exposures.watch.sync

class OfflineModeQueueFlusher(
    private val exposurePusher: ExposurePusher,
    private val rollCompletionSender: RollCompletionSender,
    private val rollsSyncRequestSender: RollsSyncRequestSender,
) {
    suspend fun flushAll() {
        exposurePusher.flushPending()
        rollCompletionSender.flushPending()
        rollsSyncRequestSender.flushPending()
    }
}
