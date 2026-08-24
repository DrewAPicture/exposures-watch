package com.exposures.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.exposures.database.repository.ExposureRepository
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.OfflineModeQueueFlusher
import com.exposures.watch.sync.FilmMediaSyncRequestSender
import com.exposures.watch.sync.FilmMediumCompletionSender
import com.exposures.watch.settings.OfflineModePreferences
import com.exposures.watch.ui.exposureentry.ExposureEntryViewModel
import com.exposures.watch.ui.framedetail.FrameDetailViewModel
import com.exposures.watch.ui.frameedit.FrameEditViewModel
import com.exposures.watch.ui.framehistory.FrameHistoryViewModel
import com.exposures.watch.ui.filmmediumdetail.FilmMediumDetailViewModel
import com.exposures.watch.ui.filmmediaswitcher.FilmMediaSwitcherViewModel
import com.exposures.watch.ui.settings.WatchSettingsViewModel

/**
 * Manual ViewModel factory standing in for Hilt (see [AppContainer]). [filmMediumId]/[exposureId]
 * are only required by the ViewModels that need them; each screen passes just what it has.
 */
class ExposuresViewModelFactory(
    private val repository: ExposureRepository,
    private val exposurePusher: ExposurePusher,
    private val filmMediumCompletionSender: FilmMediumCompletionSender,
    private val filmMediaSyncRequestSender: FilmMediaSyncRequestSender,
    private val offlineModePreferences: OfflineModePreferences? = null,
    private val offlineModeQueueFlusher: OfflineModeQueueFlusher? = null,
    private val filmMediumId: String? = null,
    private val exposureId: String? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        FilmMediaSwitcherViewModel::class.java -> FilmMediaSwitcherViewModel(repository, filmMediaSyncRequestSender, filmMediumCompletionSender)
        FilmMediumDetailViewModel::class.java -> FilmMediumDetailViewModel(repository, requireNotNull(filmMediumId))
        ExposureEntryViewModel::class.java ->
            ExposureEntryViewModel(repository, exposurePusher, filmMediumCompletionSender, requireNotNull(filmMediumId))
        FrameHistoryViewModel::class.java -> FrameHistoryViewModel(repository, exposurePusher, requireNotNull(filmMediumId))
        FrameDetailViewModel::class.java -> FrameDetailViewModel(repository, requireNotNull(exposureId))
        FrameEditViewModel::class.java -> FrameEditViewModel(repository, exposurePusher, requireNotNull(exposureId))
        WatchSettingsViewModel::class.java ->
            WatchSettingsViewModel(requireNotNull(offlineModePreferences), requireNotNull(offlineModeQueueFlusher))
        else -> error("Unknown ViewModel class: $modelClass")
    } as T
}
