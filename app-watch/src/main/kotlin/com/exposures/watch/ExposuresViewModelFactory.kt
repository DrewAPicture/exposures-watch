package com.exposures.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.exposures.database.repository.ExposureRepository
import com.exposures.watch.sync.CaptureRequestSender
import com.exposures.watch.sync.ExposurePusher
import com.exposures.watch.sync.RollCompletionSender
import com.exposures.watch.ui.exposureentry.ExposureEntryViewModel
import com.exposures.watch.ui.framedetail.FrameDetailViewModel
import com.exposures.watch.ui.framehistory.FrameHistoryViewModel
import com.exposures.watch.ui.rolldetail.RollDetailViewModel
import com.exposures.watch.ui.rollswitcher.RollSwitcherViewModel

/**
 * Manual ViewModel factory standing in for Hilt (see [AppContainer]). [rollId]/[exposureId] are
 * only required by the ViewModels that need them; each screen passes just what it has.
 */
class ExposuresViewModelFactory(
    private val repository: ExposureRepository,
    private val exposurePusher: ExposurePusher,
    private val captureRequestSender: CaptureRequestSender,
    private val rollCompletionSender: RollCompletionSender,
    private val rollId: String? = null,
    private val exposureId: String? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        RollSwitcherViewModel::class.java -> RollSwitcherViewModel(repository)
        RollDetailViewModel::class.java -> RollDetailViewModel(repository, requireNotNull(rollId))
        ExposureEntryViewModel::class.java ->
            ExposureEntryViewModel(repository, exposurePusher, captureRequestSender, rollCompletionSender, requireNotNull(rollId))
        FrameHistoryViewModel::class.java -> FrameHistoryViewModel(repository, requireNotNull(rollId))
        FrameDetailViewModel::class.java -> FrameDetailViewModel(repository, requireNotNull(exposureId))
        else -> error("Unknown ViewModel class: $modelClass")
    } as T
}
