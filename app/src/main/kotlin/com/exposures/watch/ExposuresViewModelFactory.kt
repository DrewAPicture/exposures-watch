package com.exposures.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.exposures.database.repository.ExposureRepository
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.OfflineModeQueueFlusher
import com.exposures.watch.sync.RollsSyncRequestSender
import com.exposures.watch.sync.RollCompletionSender
import com.exposures.watch.settings.OfflineModePreferences
import com.exposures.watch.ui.exposureentry.ExposureEntryViewModel
import com.exposures.watch.ui.framedetail.FrameDetailViewModel
import com.exposures.watch.ui.frameedit.FrameEditViewModel
import com.exposures.watch.ui.framehistory.FrameHistoryViewModel
import com.exposures.watch.ui.rolldetail.RollDetailViewModel
import com.exposures.watch.ui.rollswitcher.RollSwitcherViewModel
import com.exposures.watch.ui.settings.WatchSettingsViewModel

/**
 * Manual ViewModel factory standing in for Hilt (see [AppContainer]). [rollId]/[exposureId] are
 * only required by the ViewModels that need them; each screen passes just what it has.
 */
class ExposuresViewModelFactory(
    private val repository: ExposureRepository,
    private val exposurePusher: ExposurePusher,
    private val rollCompletionSender: RollCompletionSender,
    private val rollsSyncRequestSender: RollsSyncRequestSender,
    private val offlineModePreferences: OfflineModePreferences? = null,
    private val offlineModeQueueFlusher: OfflineModeQueueFlusher? = null,
    private val rollId: String? = null,
    private val exposureId: String? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        RollSwitcherViewModel::class.java -> RollSwitcherViewModel(repository, rollsSyncRequestSender, rollCompletionSender)
        RollDetailViewModel::class.java -> RollDetailViewModel(repository, requireNotNull(rollId))
        ExposureEntryViewModel::class.java ->
            ExposureEntryViewModel(repository, exposurePusher, rollCompletionSender, requireNotNull(rollId))
        FrameHistoryViewModel::class.java -> FrameHistoryViewModel(repository, exposurePusher, requireNotNull(rollId))
        FrameDetailViewModel::class.java -> FrameDetailViewModel(repository, requireNotNull(exposureId))
        FrameEditViewModel::class.java -> FrameEditViewModel(repository, exposurePusher, requireNotNull(exposureId))
        WatchSettingsViewModel::class.java ->
            WatchSettingsViewModel(requireNotNull(offlineModePreferences), requireNotNull(offlineModeQueueFlusher))
        else -> error("Unknown ViewModel class: $modelClass")
    } as T
}
