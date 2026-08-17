package com.exposures.watch.ui.rollswitcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.FilmRoll
import com.exposures.watch.sync.RollCompletionSender
import com.exposures.watch.sync.RollsSyncRequestSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RollSwitcherUiState(
    val isLoading: Boolean = true,
    val rolls: List<FilmRoll> = emptyList(),
    val activeRollId: String? = null,
    val refreshInFlight: Boolean = false,
    val refreshFailed: Boolean = false,
    /** Roll pending a long-press "complete this roll?" confirmation, if any. */
    val pendingCompleteRollId: String? = null,
    val completeRollFailed: Boolean = false,
)

class RollSwitcherViewModel(
    private val repository: ExposureRepository,
    private val rollsSyncRequestSender: RollsSyncRequestSender,
    private val rollCompletionSender: RollCompletionSender,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState())
    private val completeRollState = MutableStateFlow(CompleteRollState())

    val uiState: StateFlow<RollSwitcherUiState> = combine(
        repository.observeAvailableRolls(),
        repository.observeActiveRollId(),
        refreshState,
        completeRollState,
    ) { rolls, activeRollId, refresh, completeRoll ->
        RollSwitcherUiState(
            isLoading = false,
            rolls = rolls,
            activeRollId = activeRollId,
            refreshInFlight = refresh.inFlight,
            refreshFailed = refresh.failed,
            pendingCompleteRollId = completeRoll.pendingRollId,
            completeRollFailed = completeRoll.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RollSwitcherUiState())

    fun selectRoll(rollId: String) {
        viewModelScope.launch { repository.setActiveRoll(rollId) }
    }

    fun refreshFromPhone() {
        viewModelScope.launch {
            refreshState.update { it.copy(inFlight = true, failed = false) }
            val success = rollsSyncRequestSender.requestRefresh()
            refreshState.update { it.copy(inFlight = false, failed = !success) }
        }
    }

    fun dismissRefreshFailure() {
        refreshState.update { it.copy(failed = false) }
    }

    /** Long-press on a roll opens this confirmation — for finishing a roll early, before its last frame. */
    fun requestCompleteRoll(rollId: String) {
        completeRollState.update { it.copy(pendingRollId = rollId, failed = false) }
    }

    fun cancelCompleteRoll() {
        completeRollState.update { it.copy(pendingRollId = null) }
    }

    fun confirmCompleteRoll() {
        val rollId = completeRollState.value.pendingRollId ?: return
        viewModelScope.launch {
            val sent = rollCompletionSender.complete(rollId)
            completeRollState.update {
                if (sent) CompleteRollState() else it.copy(pendingRollId = null, failed = true)
            }
        }
    }

    fun dismissCompleteRollFailure() {
        completeRollState.update { it.copy(failed = false) }
    }

    private data class RefreshState(
        val inFlight: Boolean = false,
        val failed: Boolean = false,
    )

    private data class CompleteRollState(
        val pendingRollId: String? = null,
        val failed: Boolean = false,
    )
}
