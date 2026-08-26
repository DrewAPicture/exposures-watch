package com.exposures.watch.ui.components

import com.exposures.model.Lens
import com.exposures.model.LensType

/**
 * "110mm" for a fixed [LensType.PRIME] focal length, "70-200mm" for a [LensType.ZOOM] range —
 * falls back to a placeholder for a lens saved before focal length was captured (its focal length
 * fields are still null).
 */
fun Lens.focalLengthLabel(): String = when (lensType) {
    LensType.PRIME -> focalLengthMm?.let { "${it}mm" }
    LensType.ZOOM -> {
        val min = focalLengthMinMm
        val max = focalLengthMaxMm
        if (min != null && max != null) "$min-${max}mm" else null
    }
} ?: "Unknown focal length"

/**
 * Lowest to highest by starting focal length — the fixed value for [LensType.PRIME], the minimum
 * of the range for [LensType.ZOOM] — breaking ties by name. A lens with no focal length data
 * sorts to the end rather than the front.
 */
fun List<Lens>.sortedByFocalLength(): List<Lens> = sortedWith(
    compareBy(
        { lens -> (if (lens.lensType == LensType.PRIME) lens.focalLengthMm else lens.focalLengthMinMm) ?: Int.MAX_VALUE },
        { it.name },
    ),
)
