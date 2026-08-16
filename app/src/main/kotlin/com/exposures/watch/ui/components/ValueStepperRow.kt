package com.exposures.watch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * A labeled value with previous/next stepping — the picker pattern for Phase 1's discrete,
 * non-linear scales (shutter speed, aperture, lens, ISO), instead of Wear Compose's numeric
 * [androidx.wear.compose.material.Stepper], which doesn't fit non-numeric progressions directly.
 */
@Composable
fun ValueStepperRow(
    label: String,
    value: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    hasPrevious: Boolean = true,
    hasNext: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.caption2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactButton(onClick = onPrevious, enabled = hasPrevious) {
                Text("-")
            }
            Text(text = value, style = MaterialTheme.typography.button)
            CompactButton(onClick = onNext, enabled = hasNext) {
                Text("+")
            }
        }
    }
}
