package com.exposures.watch.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfflineModePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(readEnabled())
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_MODE, value).apply()
        _enabled.value = value
    }

    fun isEnabledNow(): Boolean = _enabled.value

    private fun readEnabled(): Boolean = prefs.getBoolean(KEY_OFFLINE_MODE, false)

    private companion object {
        const val PREFS_NAME = "watch_settings"
        const val KEY_OFFLINE_MODE = "offline_mode"
    }
}
