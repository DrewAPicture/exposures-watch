package com.exposures.watch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.exposures.database.repository.ExposureRepository
import com.exposures.watch.ExposuresApplication

@Composable
fun appRepository(): ExposureRepository =
    (LocalContext.current.applicationContext as ExposuresApplication).container.repository
