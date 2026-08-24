package com.exposures.watch.sync

class OfflineModeQueueFlusher(
    private val exposurePusher: ExposurePusher,
    private val filmMediumCompletionSender: FilmMediumCompletionSender,
    private val filmMediaSyncRequestSender: FilmMediaSyncRequestSender,
) {
    suspend fun flushAll() {
        exposurePusher.flushPending()
        filmMediumCompletionSender.flushPending()
        filmMediaSyncRequestSender.flushPending()
    }
}
