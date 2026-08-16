package com.exposures.watch.tile

import com.exposures.model.FilmRoll

/** What the quick-logging Tile shows — resolved here so it's testable independent of protolayout. */
data class TileContent(
    val rollId: String?,
    val headline: String,
    val subline: String,
)

object TileContentResolver {
    fun resolve(activeRoll: FilmRoll?, exposureCount: Int): TileContent = if (activeRoll == null) {
        TileContent(rollId = null, headline = "No active roll", subline = "Open the app to pick one")
    } else {
        TileContent(
            rollId = activeRoll.id,
            headline = activeRoll.name,
            subline = "$exposureCount / ${activeRoll.targetFrameCount} frames",
        )
    }
}
