package com.exposures.watch.sync

class OfflineModeQueueFlusher(
    private val exposurePusher: ExposurePusher,
    private val captureRequestSender: CaptureRequestSender,
    private val rollCompletionSender: RollCompletionSender,
    private val rollsSyncRequestSender: RollsSyncRequestSender,
) {
    suspend fun flushAll() {
        exposurePusher.flushPending()
        captureRequestSender.flushPending()
        rollCompletionSender.flushPending()
        rollsSyncRequestSender.flushPending()
    }
}
