package com.exposures.watch.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.Text

@Composable
fun OfflineIndicatorTimeText() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Offline mode enabled" },
        contentAlignment = Alignment.TopCenter,
    ) {
        Text("⛔")
    }
}
