package com.exposures.watch.ui.components

import com.exposures.model.Lens
import com.exposures.model.LensType
import com.exposures.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LensDisplayTest {

    private fun primeLens(id: String = "lens-1", name: String = "Test Lens", focalLengthMm: Int? = 50) = Lens(
        id = id, name = name, cameraBodyId = null, minAperture = 1.8, maxAperture = 22.0,
        stopIncrement = com.exposures.model.StopIncrement.HALF_STOP, referencePhotoZoomRatio = 1.0,
        lensType = LensType.PRIME, focalLengthMm = focalLengthMm,
        createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
    )

    private fun zoomLens(
        id: String = "lens-zoom",
        name: String = "Test Zoom",
        minMm: Int? = 70,
        maxMm: Int? = 200,
    ) = Lens(
        id = id, name = name, cameraBodyId = null, minAperture = 2.8, maxAperture = 22.0,
        stopIncrement = com.exposures.model.StopIncrement.HALF_STOP, referencePhotoZoomRatio = 1.0,
        lensType = LensType.ZOOM, focalLengthMinMm = minMm, focalLengthMaxMm = maxMm,
        createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
    )

    @Test
    fun `focalLengthLabel formats a prime lens as a single mm value`() {
        assertEquals("50mm", primeLens(focalLengthMm = 50).focalLengthLabel())
    }

    @Test
    fun `focalLengthLabel formats a zoom lens as a range`() {
        assertEquals("70-200mm", zoomLens(minMm = 70, maxMm = 200).focalLengthLabel())
    }

    @Test
    fun `focalLengthLabel falls back for a prime lens with no focal length recorded`() {
        assertEquals("Unknown focal length", primeLens(focalLengthMm = null).focalLengthLabel())
    }

    @Test
    fun `focalLengthLabel falls back for a zoom lens missing either end of its range`() {
        assertEquals("Unknown focal length", zoomLens(minMm = 70, maxMm = null).focalLengthLabel())
    }

    @Test
    fun `sortedByFocalLength orders lowest to highest by starting focal length`() {
        val wide = primeLens(id = "wide", focalLengthMm = 24)
        val normal = primeLens(id = "normal", focalLengthMm = 50)
        val tele = zoomLens(id = "tele", minMm = 70, maxMm = 200)

        val sorted = listOf(tele, wide, normal).sortedByFocalLength()

        assertEquals(listOf("wide", "normal", "tele"), sorted.map { it.id })
    }

    @Test
    fun `sortedByFocalLength breaks ties at the same starting focal length by name`() {
        val zoomStartingAt50 = zoomLens(id = "zoom-50", name = "Z Lens", minMm = 50, maxMm = 100)
        val primeAt50 = primeLens(id = "prime-50", name = "A Lens", focalLengthMm = 50)

        val sorted = listOf(zoomStartingAt50, primeAt50).sortedByFocalLength()

        assertEquals(listOf("prime-50", "zoom-50"), sorted.map { it.id })
    }

    @Test
    fun `sortedByFocalLength puts lenses with no focal length data at the end`() {
        val known = primeLens(id = "known", focalLengthMm = 35)
        val unknown = primeLens(id = "unknown", focalLengthMm = null)

        val sorted = listOf(unknown, known).sortedByFocalLength()

        assertEquals(listOf("known", "unknown"), sorted.map { it.id })
    }
}
