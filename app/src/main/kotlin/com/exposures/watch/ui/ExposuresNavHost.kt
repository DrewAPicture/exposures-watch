package com.exposures.watch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.exposures.watch.ui.exposureentry.ExposureEntryScreen
import com.exposures.watch.ui.framedetail.FrameDetailScreen
import com.exposures.watch.ui.framehistory.FrameHistoryScreen
import com.exposures.watch.ui.rolldetail.RollDetailScreen
import com.exposures.watch.ui.rollswitcher.RollSwitcherScreen

// ExposureEntryScreen owns both the picker step and the confirm step as one ViewModel-driven state
// machine rather than two nav destinations, so there's no need to share a ViewModel across a
// parent/child back-stack entry — see the note on ExposureEntryViewModel.
@Composable
fun ExposuresNavHost(startExposureEntryRollId: String? = null) {
    val navController = rememberSwipeDismissableNavController()

    // RollSwitcherScreen stays the actual start destination so the back stack — and therefore
    // ExposureEntryScreen's existing onSaved/onRollCompleted pop behavior — works exactly like the
    // normal flow; this just pushes exposure entry on top of it as soon as the app opens.
    LaunchedEffect(startExposureEntryRollId) {
        if (startExposureEntryRollId != null) {
            navController.navigate(Routes.exposureEntry(startExposureEntryRollId))
        }
    }

    SwipeDismissableNavHost(navController = navController, startDestination = Routes.ROLL_SWITCHER) {
        composable(Routes.ROLL_SWITCHER) {
            RollSwitcherScreen(onRollSelected = { rollId -> navController.navigate(Routes.rollDetail(rollId)) })
        }
        composable(Routes.ROLL_DETAIL) { backStackEntry ->
            val rollId = requireNotNull(backStackEntry.arguments?.getString(Routes.ARG_ROLL_ID))
            RollDetailScreen(
                rollId = rollId,
                onLogExposure = { navController.navigate(Routes.exposureEntry(rollId)) },
                onViewHistory = { navController.navigate(Routes.frameHistory(rollId)) },
            )
        }
        composable(Routes.EXPOSURE_ENTRY) { backStackEntry ->
            val rollId = requireNotNull(backStackEntry.arguments?.getString(Routes.ARG_ROLL_ID))
            ExposureEntryScreen(
                rollId = rollId,
                onSaved = { navController.popBackStack() },
                onRollCompleted = { navController.popBackStack(Routes.ROLL_SWITCHER, false) },
            )
        }
        composable(Routes.FRAME_HISTORY) { backStackEntry ->
            val rollId = requireNotNull(backStackEntry.arguments?.getString(Routes.ARG_ROLL_ID))
            FrameHistoryScreen(
                rollId = rollId,
                onFrameSelected = { exposureId -> navController.navigate(Routes.frameDetail(exposureId)) },
            )
        }
        composable(Routes.FRAME_DETAIL) { backStackEntry ->
            val exposureId = requireNotNull(backStackEntry.arguments?.getString(Routes.ARG_EXPOSURE_ID))
            FrameDetailScreen(exposureId = exposureId)
        }
    }
}
