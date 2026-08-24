package com.exposures.watch.sync

import android.content.Context
import android.content.SharedPreferences

/** Persists deferred phone-bound command actions while Offline Mode is enabled. */
class OfflineActionQueue(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun enqueueFilmMediumCompletion(filmMediumId: String) {
        val pending = pendingFilmMediumCompletions().toMutableSet()
        pending.add(filmMediumId)
        prefs.edit().putStringSet(KEY_PENDING_FILM_MEDIUM_COMPLETIONS, pending).apply()
    }

    fun pendingFilmMediumCompletions(): Set<String> = prefs.getStringSet(KEY_PENDING_FILM_MEDIUM_COMPLETIONS, emptySet()).orEmpty()

    fun removeFilmMediumCompletion(filmMediumId: String) {
        val pending = pendingFilmMediumCompletions().toMutableSet()
        pending.remove(filmMediumId)
        prefs.edit().putStringSet(KEY_PENDING_FILM_MEDIUM_COMPLETIONS, pending).apply()
    }

    fun markRefreshRequested() {
        prefs.edit().putBoolean(KEY_PENDING_REFRESH, true).apply()
    }

    fun hasPendingRefresh(): Boolean = prefs.getBoolean(KEY_PENDING_REFRESH, false)

    fun clearPendingRefresh() {
        prefs.edit().putBoolean(KEY_PENDING_REFRESH, false).apply()
    }

    fun markExposurePushPending() {
        prefs.edit().putBoolean(KEY_PENDING_EXPOSURE_PUSH, true).apply()
    }

    fun hasPendingExposurePush(): Boolean = prefs.getBoolean(KEY_PENDING_EXPOSURE_PUSH, false)

    fun clearPendingExposurePush() {
        prefs.edit().putBoolean(KEY_PENDING_EXPOSURE_PUSH, false).apply()
    }

    private companion object {
        const val PREFS_NAME = "watch_offline_queue"
        const val KEY_PENDING_FILM_MEDIUM_COMPLETIONS = "pending_film_medium_completions"
        const val KEY_PENDING_REFRESH = "pending_refresh"
        const val KEY_PENDING_EXPOSURE_PUSH = "pending_exposure_push"
    }
}
