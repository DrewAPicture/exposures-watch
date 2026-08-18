package com.exposures.watch.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape

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
            Button(
                label = { Text("Select Roll", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                colors = ButtonDefaults.buttonColors(),
                onClick = onSelectRoll,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onOpenSettings,
                label = {
                    Text("⚙")
                },
                colors = ButtonDefaults.filledTonalButtonColors(),
                shape = CircleShape,
                modifier = Modifier.size(56.dp).semantics { contentDescription = "Settings" },
            )
        }
    }
}
