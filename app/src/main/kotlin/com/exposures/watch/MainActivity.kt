package com.exposures.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.exposures.watch.ui.ExposuresNavHost
import com.exposures.watch.ui.theme.ExposuresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExposuresTheme {
                ExposuresNavHost()
            }
        }
    }
}
