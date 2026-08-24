package com.exposures.watch.ui.filmmediaswitcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.ExposureRepository
import com.exposures.model.FilmMedium
import com.exposures.watch.sync.FilmMediumCompletionSender
import com.exposures.watch.sync.FilmMediaSyncRequestSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FilmMediaSwitcherUiState(
    val isLoading: Boolean = true,
    val filmMedia: List<FilmMedium> = emptyList(),
    val activeFilmMediumId: String? = null,
    val refreshInFlight: Boolean = false,
    val refreshFailed: Boolean = false,
    /** Film medium pending a long-press "complete this film?" confirmation, if any. */
    val pendingCompleteFilmMediumId: String? = null,
    val completeFilmMediumFailed: Boolean = false,
) {
    /** Which film medium's page the switcher pager should open on — the active one, or [filmMedia]'s first (already alphabetical) if the active one isn't among them. */
    val initialFilmMediumId: String?
        get() = filmMedia.firstOrNull { it.id == activeFilmMediumId }?.id ?: filmMedia.firstOrNull()?.id
}

class FilmMediaSwitcherViewModel(
    private val repository: ExposureRepository,
    private val filmMediaSyncRequestSender: FilmMediaSyncRequestSender,
    private val filmMediumCompletionSender: FilmMediumCompletionSender,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState())
    private val completeFilmMediumState = MutableStateFlow(CompleteFilmMediumState())

    val uiState: StateFlow<FilmMediaSwitcherUiState> = combine(
        repository.observeSwitcherFilmMedia(),
        repository.observeActiveFilmMediumId(),
        refreshState,
        completeFilmMediumState,
    ) { filmMedia, activeFilmMediumId, refresh, completeFilmMedium ->
        FilmMediaSwitcherUiState(
            isLoading = false,
            filmMedia = filmMedia,
            activeFilmMediumId = activeFilmMediumId,
            refreshInFlight = refresh.inFlight,
            refreshFailed = refresh.failed,
            pendingCompleteFilmMediumId = completeFilmMedium.pendingFilmMediumId,
            completeFilmMediumFailed = completeFilmMedium.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FilmMediaSwitcherUiState())

    fun selectFilmMedium(filmMediumId: String) {
        viewModelScope.launch { repository.setActiveFilmMedium(filmMediumId) }
    }

    fun refreshFromPhone() {
        viewModelScope.launch {
            refreshState.update { it.copy(inFlight = true, failed = false) }
            val success = filmMediaSyncRequestSender.requestRefresh()
            refreshState.update { it.copy(inFlight = false, failed = !success) }
        }
    }

    fun dismissRefreshFailure() {
        refreshState.update { it.copy(failed = false) }
    }

    /** Long-press on a film medium opens this confirmation — for finishing it early, before its last frame. */
    fun requestCompleteFilmMedium(filmMediumId: String) {
        completeFilmMediumState.update { it.copy(pendingFilmMediumId = filmMediumId, failed = false) }
    }

    fun cancelCompleteFilmMedium() {
        completeFilmMediumState.update { it.copy(pendingFilmMediumId = null) }
    }

    fun confirmCompleteFilmMedium() {
        val filmMediumId = completeFilmMediumState.value.pendingFilmMediumId ?: return
        viewModelScope.launch {
            repository.markFilmMediumCompletedLocally(filmMediumId)
            val sent = filmMediumCompletionSender.complete(filmMediumId)
            completeFilmMediumState.update {
                if (sent) CompleteFilmMediumState() else it.copy(pendingFilmMediumId = null, failed = true)
            }
        }
    }

    fun dismissCompleteFilmMediumFailure() {
        completeFilmMediumState.update { it.copy(failed = false) }
    }

    private data class RefreshState(
        val inFlight: Boolean = false,
        val failed: Boolean = false,
    )

    private data class CompleteFilmMediumState(
        val pendingFilmMediumId: String? = null,
        val failed: Boolean = false,
    )
}
