package com.exposures.watch.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var showContent by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(SPLASH_MIN_DURATION_MS)
        showContent = false
        delay(SPLASH_FADE_OUT_MS)
        onFinished()
    }

    ScreenScaffold {
        AnimatedVisibility(visible = showContent, exit = fadeOut()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("◎", style = MaterialTheme.typography.displaySmall)
                Text("Exposures", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private const val SPLASH_MIN_DURATION_MS = 1_100L
private const val SPLASH_FADE_OUT_MS = 180L
