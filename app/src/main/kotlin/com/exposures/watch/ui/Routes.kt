package com.exposures.watch.ui

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val ROLL_SWITCHER = "rollSwitcher"
    const val ROLL_DETAIL = "rollDetail/{rollId}"
    const val EXPOSURE_ENTRY = "exposureEntry/{rollId}"
    const val FRAME_HISTORY = "frameHistory/{rollId}"
    const val FRAME_DETAIL = "frameDetail/{exposureId}"
    const val FRAME_EDIT = "frameEdit/{exposureId}"

    const val ARG_ROLL_ID = "rollId"
    const val ARG_EXPOSURE_ID = "exposureId"

    fun rollDetail(rollId: String) = "rollDetail/$rollId"
    fun exposureEntry(rollId: String) = "exposureEntry/$rollId"
    fun frameHistory(rollId: String) = "frameHistory/$rollId"
    fun frameDetail(exposureId: String) = "frameDetail/$exposureId"
    fun frameEdit(exposureId: String) = "frameEdit/$exposureId"
}
