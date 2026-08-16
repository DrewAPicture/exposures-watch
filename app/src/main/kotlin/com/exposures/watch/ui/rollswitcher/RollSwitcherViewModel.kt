package com.exposures.watch.ui.rollswitcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.FilmRoll
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
)

class RollSwitcherViewModel(
    private val repository: ExposureRepository,
    private val rollsSyncRequestSender: RollsSyncRequestSender,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState())

    val uiState: StateFlow<RollSwitcherUiState> = combine(
        repository.observeAvailableRolls(),
        repository.observeActiveRollId(),
        refreshState,
    ) { rolls, activeRollId, refresh ->
        RollSwitcherUiState(
            isLoading = false,
            rolls = rolls,
            activeRollId = activeRollId,
            refreshInFlight = refresh.inFlight,
            refreshFailed = refresh.failed,
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

    private data class RefreshState(
        val inFlight: Boolean = false,
        val failed: Boolean = false,
    )
}
