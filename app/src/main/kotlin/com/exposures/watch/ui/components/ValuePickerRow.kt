package com.exposures.watch.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState

/**
 * A labeled [Picker] for choosing one of [items] by index. [selectedIndex] is the source of
 * truth (a value from the backing view model, `-1` meaning nothing selected yet); scrolling the
 * picker reports the new index via [onSelectedIndexChange] rather than mutating anything locally.
 */
@Composable
fun ValuePickerRow(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        if (items.isEmpty()) {
            Text(text = "None available", style = MaterialTheme.typography.bodySmall)
            return@Column
        }

        val initialIndex = selectedIndex.coerceIn(0, items.lastIndex)
        val pickerState = rememberPickerState(items.size, initialIndex, false)

        LaunchedEffect(items.size) {
            pickerState.numberOfOptions = items.size
        }
        LaunchedEffect(selectedIndex, items) {
            val target = selectedIndex.coerceIn(0, items.lastIndex)
            if (pickerState.selectedOptionIndex != target) pickerState.scrollToOption(target)
        }
        LaunchedEffect(pickerState.selectedOptionIndex, items) {
            val current = pickerState.selectedOptionIndex
            if (current in items.indices && current != selectedIndex) onSelectedIndexChange(current)
        }

        Picker(
            state = pickerState,
            contentDescription = { items.getOrElse(pickerState.selectedOptionIndex) { label } },
            modifier = Modifier.fillMaxWidth().height(108.dp),
            gradientRatio = 0.4f,
        ) { optionIndex ->
            val isSelected = optionIndex == pickerState.selectedOptionIndex
            Text(
                text = items[optionIndex],
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                },
            )
        }
    }
}
