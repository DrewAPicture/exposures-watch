package com.exposures.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.exposures.watch.ui.ExposuresNavHost
import com.exposures.watch.ui.theme.ExposuresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set by ExposuresTileService's launch action when opened via the quick-logging Tile —
        // jumps straight into exposure entry for that roll instead of the usual roll switcher.
        val startExposureEntryRollId = intent.getStringExtra(EXTRA_ROLL_ID)?.takeIf { it.isNotBlank() }
        setContent {
            ExposuresTheme {
                ExposuresNavHost(startExposureEntryRollId = startExposureEntryRollId)
            }
        }
    }

    companion object {
        const val EXTRA_ROLL_ID = "com.exposures.watch.EXTRA_ROLL_ID"
    }
}
