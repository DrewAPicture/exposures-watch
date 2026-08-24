package com.exposures.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import com.exposures.watch.ui.ExposuresNavHost
import com.exposures.watch.ui.components.OfflineIndicatorTimeText
import com.exposures.watch.ui.theme.ExposuresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set by ExposuresTileService's launch action when opened via the quick-logging Tile —
        // jumps straight into exposure entry for that film medium instead of the usual switcher.
        val startExposureEntryFilmMediumId = intent.getStringExtra(EXTRA_FILM_MEDIUM_ID)?.takeIf { it.isNotBlank() }
        // Set by ExposuresTileService's launch action when opened via the Select Film tile —
        // jumps straight to the film-media picker instead of Home.
        val startAtFilmMediaSwitcher = intent.getBooleanExtra(EXTRA_START_FILM_MEDIA_SWITCHER, false)
        setContent {
            val offlineModeEnabled by (application as ExposuresApplication)
                .container
                .offlineModePreferences
                .enabled
                .collectAsState()
            ExposuresTheme {
                AppScaffold(
                    timeText = {
                        if (offlineModeEnabled) {
                            OfflineIndicatorTimeText()
                        } else {
                            TimeText()
                        }
                    },
                ) {
                    ExposuresNavHost(
                        startExposureEntryFilmMediumId = startExposureEntryFilmMediumId,
                        startAtFilmMediaSwitcher = startAtFilmMediaSwitcher,
                    )
                }
            }
        }
    }

    companion object {
        private val runtimePackageName: String =
            MainActivity::class.java.`package`?.name ?: "com.exposures.watch"
        val EXTRA_FILM_MEDIUM_ID: String = "$runtimePackageName.EXTRA_FILM_MEDIUM_ID"
        val EXTRA_START_FILM_MEDIA_SWITCHER: String = "$runtimePackageName.EXTRA_START_FILM_MEDIA_SWITCHER"
    }
}
