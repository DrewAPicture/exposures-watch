package com.exposures.watch.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.exposures.watch.ui.exposureentry.ExposureEntryScreen
import com.exposures.watch.ui.framedetail.FrameDetailScreen
import com.exposures.watch.ui.frameedit.FrameEditScreen
import com.exposures.watch.ui.framehistory.FrameHistoryScreen
import com.exposures.watch.ui.home.HomeScreen
import com.exposures.watch.ui.home.SplashScreen
import com.exposures.watch.ui.rolldetail.RollDetailScreen
import com.exposures.watch.ui.rollswitcher.RollSwitcherScreen
import com.exposures.watch.ui.settings.SettingsScreen

@Composable
fun ExposuresNavHost(
    startExposureEntryRollId: String? = null,
    startAtRollSwitcher: Boolean = false,
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    when {
                        startExposureEntryRollId != null -> {
                            navController.navigate(Routes.ROLL_SWITCHER) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                            navController.navigate(Routes.exposureEntry(startExposureEntryRollId))
                        }
                        // Set by ExposuresTileService's launch action — the tile is a "Select
                        // Roll" quick-launcher, not a specific-roll shortcut, so it goes straight
                        // to the picker rather than Home (which would just make you tap Select
                        // Roll a second time) or a specific roll's exposure entry.
                        startAtRollSwitcher -> {
                            navController.navigate(Routes.ROLL_SWITCHER) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                        else -> {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onSelectRoll = { navController.navigate(Routes.ROLL_SWITCHER) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
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
                onViewHistory = { navController.navigate(Routes.frameHistory(rollId)) },
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
            FrameDetailScreen(
                exposureId = exposureId,
                onEdit = { navController.navigate(Routes.frameEdit(exposureId)) },
            )
        }
        composable(Routes.FRAME_EDIT) { backStackEntry ->
            val exposureId = requireNotNull(backStackEntry.arguments?.getString(Routes.ARG_EXPOSURE_ID))
            FrameEditScreen(exposureId = exposureId, onSaved = { navController.popBackStack() })
        }
    }
}
