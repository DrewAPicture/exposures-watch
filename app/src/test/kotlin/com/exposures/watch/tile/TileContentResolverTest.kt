package com.exposures.watch.tile

import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.RollStatus
import com.exposures.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileContentResolverTest {

    private fun roll(targetFrameCount: Int = 10) = FilmRoll(
        id = "roll-1",
        name = "Portra 400 — Roll 1",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        colorType = FilmColorType.COLOR,
        cameraBodyId = "body-1",
        lightMeterId = null,
        targetFrameCount = targetFrameCount,
        status = RollStatus.AVAILABLE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    @Test
    fun `no active roll shows an empty-state prompt and no roll id`() {
        val content = TileContentResolver.resolve(activeRoll = null, exposureCount = 0)

        assertNull(content.rollId)
        assertEquals("No active roll", content.headline)
    }

    @Test
    fun `an active roll shows its name and frame progress`() {
        val content = TileContentResolver.resolve(activeRoll = roll(targetFrameCount = 10), exposureCount = 3)

        assertEquals("roll-1", content.rollId)
        assertEquals("Portra 400 — Roll 1", content.headline)
        assertEquals("3 / 10 frames", content.subline)
    }

    @Test
    fun `frame progress reflects zero logged exposures`() {
        val content = TileContentResolver.resolve(activeRoll = roll(targetFrameCount = 24), exposureCount = 0)

        assertEquals("0 / 24 frames", content.subline)
    }
}
