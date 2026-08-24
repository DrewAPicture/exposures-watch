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
import com.exposures.watch.ui.filmmediumdetail.FilmMediumDetailScreen
import com.exposures.watch.ui.filmmediaswitcher.FilmMediaSwitcherScreen
import com.exposures.watch.ui.settings.SettingsScreen

@Composable
fun ExposuresNavHost(
    startExposureEntryFilmMediumId: String? = null,
    startAtFilmMediaSwitcher: Boolean = false,
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    when {
                        startExposureEntryFilmMediumId != null -> {
                            navController.navigate(Routes.FILM_MEDIA_SWITCHER) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                            navController.navigate(Routes.exposureEntry(startExposureEntryFilmMediumId))
                        }
                        // Set by ExposuresTileService's launch action — the tile is a "Select
                        // Film" quick-launcher, not a specific-film shortcut, so it goes straight
                        // to the picker rather than Home (which would just make you tap Select
                        // Film a second time) or a specific film's exposure entry.
                        startAtFilmMediaSwitcher -> {
                            navController.navigate(Routes.FILM_MEDIA_SWITCHER) {
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
                onSelectFilm = { navController.navigate(Routes.FILM_MEDIA_SWITCHER) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
        composable(Routes.FILM_MEDIA_SWITCHER) {
            FilmMediaSwitcherScreen(onFilmMediumSelected = { filmMediumId -> navController.navigate(Routes.filmMediumDetail(filmMediumId)) })
        }
        composable(Routes.FILM_MEDIUM_DETAIL) { backStackEntry ->
            val filmMediumId = requireNotNull(backStackEntry.arguments?.getString(Routes.ARG_FILM_MEDIUM_ID))
            FilmMediumDetailScreen(
                filmMediumId = filmMediumId,
                onLogExposure = { navController.navigate(Routes.exposureEntry(filmMediumId)) },
                onViewHistory = { navController.navigate(Routes.frameHistory(filmMediumId)) },
            )
        }
        composable(Routes.EXPOSURE_ENTRY) { backStackEntry ->
            val filmMediumId = requireNotNull(backStackEntry.arguments?.getString(Routes.ARG_FILM_MEDIUM_ID))
            ExposureEntryScreen(
                filmMediumId = filmMediumId,
                onSaved = { navController.popBackStack() },
                onFilmMediumCompleted = { navController.popBackStack(Routes.FILM_MEDIA_SWITCHER, false) },
            )
        }
        composable(Routes.FRAME_HISTORY) { backStackEntry ->
            val filmMediumId = requireNotNull(backStackEntry.arguments?.getString(Routes.ARG_FILM_MEDIUM_ID))
            FrameHistoryScreen(
                filmMediumId = filmMediumId,
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
