package com.exposures.watch.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/** Horizontal drag distance past which a swipe on the Settings button toggles reveal/collapse. */
private val SwipeRevealThreshold = 40.dp

@Composable
fun HomeScreen(
    onSelectFilm: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            // Title floats independently of the button row below, via its own Box alignment —
            // otherwise including it in the same centered layout would push the row off true
            // center every time the title's height changes.
            Text(
                "Exposures",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // weight(1f) with no weight on the Settings button beside it: Settings measures at
                // its own intrinsic width (small icon-only, or wider once swipe-revealed) and
                // Select Film's Text ellipsizes into whatever's left, rather than the two buttons'
                // widths being explicitly linked — Compose's own Row layout gives the "combined
                // width doesn't overflow the row" behavior for free.
                Button(
                    label = { Text("Select Film", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = ButtonDefaults.buttonColors(),
                    onClick = onSelectFilm,
                    modifier = Modifier.weight(1f),
                )
                SettingsSwipeButton(onOpenSettings = onOpenSettings)
            }
        }
    }
}

/**
 * Starts as an icon-only circular button; swiping left reveals a "Settings" label (swiping right,
 * or swiping left again from the expanded state's perspective — i.e. the reverse direction,
 * collapses it back). Tapping always navigates to Settings regardless of collapsed/expanded state
 * — the swipe is a preview/reveal only, not a commit gesture.
 */
@Composable
private fun SettingsSwipeButton(onOpenSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { SwipeRevealThreshold.toPx() }

    val gestureModifier = Modifier
        .semantics { contentDescription = "Settings" }
        .pointerInput(Unit) {
            var dragTotal = 0f
            detectHorizontalDragGestures(
                onDragStart = { dragTotal = 0f },
                onDragEnd = {
                    if (dragTotal <= -thresholdPx) {
                        expanded = true
                    } else if (dragTotal >= thresholdPx) {
                        expanded = false
                    }
                },
            ) { change, dragAmount ->
                change.consume()
                dragTotal += dragAmount
            }
        }
        .animateContentSize()

    if (expanded) {
        FilledTonalButton(
            onClick = onOpenSettings,
            modifier = gestureModifier,
            icon = { Text("⚙") },
            label = { Text("Settings", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
    } else {
        FilledTonalIconButton(onClick = onOpenSettings, modifier = gestureModifier) {
            Text("⚙", style = MaterialTheme.typography.titleLarge)
        }
    }
}
