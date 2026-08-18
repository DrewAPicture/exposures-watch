package com.exposures.watch.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Composable
fun HomeScreen(
    onSelectRoll: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            // Deliberately not fillMaxWidth: this button should size to its label, not stretch
            // edge-to-edge past the round viewport's safe area.
            Button(
                label = { Text("Select Roll") },
                colors = ButtonDefaults.buttonColors(),
                onClick = onSelectRoll,
            )
            FilledTonalIconButton(
                onClick = onOpenSettings,
                modifier = Modifier.semantics { contentDescription = "Settings" },
            ) {
                Text("⚙", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
