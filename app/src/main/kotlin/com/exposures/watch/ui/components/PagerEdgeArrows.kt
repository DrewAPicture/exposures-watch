package com.exposures.watch.ui.components

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
 */
@Composable
fun PagerEdgeArrows(pagerState: PagerState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        if (pagerState.currentPage > 0) {
            Text(text = "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
        }
        if (pagerState.currentPage < pagerState.pageCount - 1) {
            Text(text = "›", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
        }
    }
}
