package com.exposures.watch.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exposures.watch.MainDispatcherRule
import com.exposures.watch.createSeededTestRepository
import com.exposures.watch.settings.OfflineModePreferences
import com.exposures.watch.sync.CaptureRequestSender
import com.exposures.watch.sync.FakeDataLayerGateway
import com.exposures.watch.sync.OfflineActionQueue
import com.exposures.watch.sync.OfflineModeQueueFlusher
import com.exposures.watch.sync.RollCompletionSender
import com.exposures.watch.sync.RollsSyncRequestSender
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WatchSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createOfflineModePreferences(initialEnabled: Boolean): OfflineModePreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_settings", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineModePreferences(context).also { it.setEnabled(initialEnabled) }
    }

    private fun createOfflineActionQueue(): OfflineActionQueue {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("watch_offline_queue", Context.MODE_PRIVATE).edit().clear().commit()
        return OfflineActionQueue(context)
    }

    @Test
    fun `offline mode defaults to disabled`() = runTest {
        val viewModel = WatchSettingsViewModel(
            offlineModePreferences = createOfflineModePreferences(initialEnabled = false),
            offlineModeQueueFlusher = OfflineModeQueueFlusher(
                exposurePusher = com.exposures.watch.sync.ExposurePusher(
                    repository = createSeededTestRepository(),
                    gateway = FakeDataLayerGateway(),
                    offlineModePreferences = createOfflineModePreferences(initialEnabled = false),
                    offlineActionQueue = createOfflineActionQueue(),
                ),
                captureRequestSender = CaptureRequestSender(
                    repository = createSeededTestRepository(),
                    gateway = FakeDataLayerGateway(),
                    offlineModePreferences = createOfflineModePreferences(initialEnabled = false),
                ),
                rollCompletionSender = RollCompletionSender(
                    gateway = FakeDataLayerGateway(),
                    offlineModePreferences = createOfflineModePreferences(initialEnabled = false),
                    offlineActionQueue = createOfflineActionQueue(),
                ),
                rollsSyncRequestSender = RollsSyncRequestSender(
                    gateway = FakeDataLayerGateway(),
                    offlineModePreferences = createOfflineModePreferences(initialEnabled = false),
                    offlineActionQueue = createOfflineActionQueue(),
                ),
            ),
        )

        val state = viewModel.uiState.first()
        assertFalse(state.offlineModeEnabled)
    }

    @Test
    fun `setOfflineModeEnabled updates the ui state`() = runTest {
        val offlineModePreferences = createOfflineModePreferences(initialEnabled = false)
        val viewModel = WatchSettingsViewModel(
            offlineModePreferences = offlineModePreferences,
            offlineModeQueueFlusher = OfflineModeQueueFlusher(
                exposurePusher = com.exposures.watch.sync.ExposurePusher(
                    repository = createSeededTestRepository(),
                    gateway = FakeDataLayerGateway(),
                    offlineModePreferences = offlineModePreferences,
                    offlineActionQueue = createOfflineActionQueue(),
                ),
                captureRequestSender = CaptureRequestSender(
                    repository = createSeededTestRepository(),
                    gateway = FakeDataLayerGateway(),
                    offlineModePreferences = offlineModePreferences,
                ),
                rollCompletionSender = RollCompletionSender(
                    gateway = FakeDataLayerGateway(),
                    offlineModePreferences = offlineModePreferences,
                    offlineActionQueue = createOfflineActionQueue(),
                ),
                rollsSyncRequestSender = RollsSyncRequestSender(
                    gateway = FakeDataLayerGateway(),
                    offlineModePreferences = offlineModePreferences,
                    offlineActionQueue = createOfflineActionQueue(),
                ),
            ),
        )

        viewModel.setOfflineModeEnabled(true)
        assertEquals(true, viewModel.uiState.first { it.offlineModeEnabled }.offlineModeEnabled)

        viewModel.setOfflineModeEnabled(false)
        assertEquals(false, viewModel.uiState.first { !it.offlineModeEnabled }.offlineModeEnabled)
    }
}
