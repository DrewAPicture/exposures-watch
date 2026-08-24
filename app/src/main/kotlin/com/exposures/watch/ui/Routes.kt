package com.exposures.watch.ui

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val FILM_MEDIA_SWITCHER = "filmMediaSwitcher"
    const val FILM_MEDIUM_DETAIL = "filmMediumDetail/{filmMediumId}"
    const val EXPOSURE_ENTRY = "exposureEntry/{filmMediumId}"
    const val FRAME_HISTORY = "frameHistory/{filmMediumId}"
    const val FRAME_DETAIL = "frameDetail/{exposureId}"
    const val FRAME_EDIT = "frameEdit/{exposureId}"

    const val ARG_FILM_MEDIUM_ID = "filmMediumId"
    const val ARG_EXPOSURE_ID = "exposureId"

    fun filmMediumDetail(filmMediumId: String) = "filmMediumDetail/$filmMediumId"
    fun exposureEntry(filmMediumId: String) = "exposureEntry/$filmMediumId"
    fun frameHistory(filmMediumId: String) = "frameHistory/$filmMediumId"
    fun frameDetail(exposureId: String) = "frameDetail/$exposureId"
    fun frameEdit(exposureId: String) = "frameEdit/$exposureId"
}
