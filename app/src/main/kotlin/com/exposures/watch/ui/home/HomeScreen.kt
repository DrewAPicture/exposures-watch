package com.exposures.watch.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
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
        Box(modifier = Modifier.fillMaxSize()) {
            // Title floats independently of the button group below, via its own Box alignment —
            // otherwise including it in the same centered Column would push Select Roll off true
            // center every time the title's height changes.
            Text(
                "Exposures",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            )
            Column(
                // A touch below true center (rather than Alignment.Center) so the button group
                // clears the title above it instead of crowding its bottom edge.
                modifier = Modifier.align(BiasAlignment(horizontalBias = 0f, verticalBias = 0.2f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                // Deliberately not fillMaxWidth: this button should size to its label, not
                // stretch edge-to-edge past the round viewport's safe area.
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
}
