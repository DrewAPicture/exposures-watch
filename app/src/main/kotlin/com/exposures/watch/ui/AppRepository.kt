package com.exposures.watch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.exposures.watch.AppContainer
import com.exposures.watch.ExposuresApplication

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as ExposuresApplication).container
