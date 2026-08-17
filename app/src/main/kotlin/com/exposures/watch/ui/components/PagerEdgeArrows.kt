package com.exposures.watch.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material3.Text

/**
 * Edge-pinned "‹"/"›" hints that a [androidx.wear.compose.foundation.pager.HorizontalPager] can be
 * swiped. [PagerState] has no scroll-availability flag (checked via javap against the resolved
 * compose-foundation 1.6.2 AAR), so this derives it from currentPage/pageCount instead.
 *
 * Long-pressing the right arrow, when [onLongClickRight] is given, skips straight to the last page
 * instead of paging through one at a time.
 */
@Composable
fun PagerEdgeArrows(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onLongClickRight: (() -> Unit)? = null,
    onLongClickRightLabel: String = "Skip to end",
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (pagerState.currentPage > 0) {
            Text(text = "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
        }
        if (pagerState.currentPage < pagerState.pageCount - 1) {
            var rightArrowModifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
            if (onLongClickRight != null) {
                rightArrowModifier = rightArrowModifier.combinedClickable(
                    onClick = {},
                    onLongClick = onLongClickRight,
                    onLongClickLabel = onLongClickRightLabel,
                )
            }
            Text(text = "›", modifier = rightArrowModifier)
        }
    }
}
