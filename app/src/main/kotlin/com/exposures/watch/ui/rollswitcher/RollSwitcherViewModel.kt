package com.exposures.watch.ui.rollswitcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.FilmRoll
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RollSwitcherUiState(
    val isLoading: Boolean = true,
    val rolls: List<FilmRoll> = emptyList(),
    val activeRollId: String? = null,
)

class RollSwitcherViewModel(private val repository: ExposureRepository) : ViewModel() {

    val uiState: StateFlow<RollSwitcherUiState> = combine(
        repository.observeAvailableRolls(),
        repository.observeActiveRollId(),
    ) { rolls, activeRollId ->
        RollSwitcherUiState(isLoading = false, rolls = rolls, activeRollId = activeRollId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RollSwitcherUiState())

    fun selectRoll(rollId: String) {
        viewModelScope.launch { repository.setActiveRoll(rollId) }
    }
}
