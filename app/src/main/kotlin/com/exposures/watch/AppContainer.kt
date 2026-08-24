package com.exposures.watch

import android.app.Application
import com.exposures.database.ExposuresDatabase
import com.exposures.database.ExposuresDatabaseProvider
import com.exposures.database.repository.ExposureRepository
import com.exposures.datalayer.DataLayerClient
import com.exposures.watch.settings.OfflineModePreferences
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.OfflineActionQueue
import com.exposures.watch.sync.OfflineModeQueueFlusher
import com.exposures.watch.sync.RollsSyncRequestSender
import com.exposures.watch.sync.RollCompletionSender

/**
 * Hand-rolled DI container. Still no Hilt — the graph is bigger now (Data Layer sync/capture
 * classes), but wiring it by hand remains simple enough not to justify the ceremony yet.
 */
interface AppContainer {
    val repository: ExposureRepository
    val dataLayerClient: DataLayerClient
    val exposurePusher: ExposurePusher
    val rollCompletionSender: RollCompletionSender
    val rollsSyncRequestSender: RollsSyncRequestSender
    val offlineModePreferences: OfflineModePreferences
    val offlineModeQueueFlusher: OfflineModeQueueFlusher
}

class DefaultAppContainer(application: Application) : AppContainer {

    private val database: ExposuresDatabase by lazy { ExposuresDatabaseProvider.create(application) }

    override val repository: ExposureRepository by lazy { ExposureRepository(database) }
    override val offlineModePreferences: OfflineModePreferences by lazy { OfflineModePreferences(application) }
    private val offlineActionQueue: OfflineActionQueue by lazy { OfflineActionQueue(application) }
    override val dataLayerClient: DataLayerClient by lazy { DataLayerClient(application) }
    override val exposurePusher: ExposurePusher by lazy {
        ExposurePusher(repository, dataLayerClient, offlineModePreferences, offlineActionQueue)
    }
    override val rollCompletionSender: RollCompletionSender by lazy {
        RollCompletionSender(dataLayerClient, offlineModePreferences, offlineActionQueue)
    }
    override val rollsSyncRequestSender: RollsSyncRequestSender by lazy {
        RollsSyncRequestSender(dataLayerClient, offlineModePreferences, offlineActionQueue)
    }
    override val offlineModeQueueFlusher: OfflineModeQueueFlusher by lazy {
        OfflineModeQueueFlusher(exposurePusher, rollCompletionSender, rollsSyncRequestSender)
    }
}
