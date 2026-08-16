package com.exposures.model

/**
 * Ansel Adams's Zone System: 11 zones from pure black (0) to pure white (X), used to decide where
 * a spot-metered reading should be placed rather than metering straight down the middle. Modeled
 * as a plain 0..10 Int on [Exposure] rather than its own enum/value class, since it's stored,
 * synced, and compared just like the other scalar exposure fields.
 */
object Zone {
    const val MIN = 0
    const val MAX = 10

    /** Where the picker starts before any exposure has ever recorded a zone. */
    const val DEFAULT = 6

    private val LABELS = listOf("0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

    /** Roman-numeral label for [zone] — Zone 0 itself is rendered "0", not a numeral. */
    fun label(zone: Int): String {
        require(zone in MIN..MAX) { "Zone must be $MIN..$MAX, was $zone" }
        return LABELS[zone]
    }
}
