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
        // jumps straight into exposure entry for that roll instead of the usual roll switcher.
        val startExposureEntryRollId = intent.getStringExtra(EXTRA_ROLL_ID)?.takeIf { it.isNotBlank() }
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
                    ExposuresNavHost(startExposureEntryRollId = startExposureEntryRollId)
                }
            }
        }
    }

    companion object {
        private val runtimePackageName: String =
            MainActivity::class.java.`package`?.name ?: "com.exposures.watch"
        val EXTRA_ROLL_ID: String = "$runtimePackageName.EXTRA_ROLL_ID"
    }
}
