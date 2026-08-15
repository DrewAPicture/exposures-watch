package com.exposures.database

import com.exposures.model.FilmFormat
import com.exposures.model.PhotoStatus
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `shutter speed round-trips through its string encoding`() {
        val original = ShutterSpeed.fraction(125)
        val encoded = converters.fromShutterSpeed(original)
        assertEquals(original, converters.toShutterSpeed(encoded))
    }

    @Test
    fun `bulb round-trips through its string encoding`() {
        val encoded = converters.fromShutterSpeed(ShutterSpeed.BULB)
        assertEquals(ShutterSpeed.BULB, converters.toShutterSpeed(encoded))
    }

    @Test
    fun `shutter speed list round-trips and preserves order`() {
        val original = listOf(ShutterSpeed.fraction(400), ShutterSpeed.wholeSeconds(2), ShutterSpeed.BULB)
        val encoded = converters.fromShutterSpeedList(original)
        assertEquals(original, converters.toShutterSpeedList(encoded))
    }

    @Test
    fun `empty shutter speed list round-trips to an empty list`() {
        assertEquals(emptyList<ShutterSpeed>(), converters.toShutterSpeedList(converters.fromShutterSpeedList(emptyList())))
    }

    @Test
    fun `enum converters round-trip every declared value`() {
        SyncStatus.entries.forEach { assertEquals(it, converters.toSyncStatus(converters.fromSyncStatus(it))) }
        RollStatus.entries.forEach { assertEquals(it, converters.toRollStatus(converters.fromRollStatus(it))) }
        PhotoStatus.entries.forEach { assertEquals(it, converters.toPhotoStatus(converters.fromPhotoStatus(it))) }
        FilmFormat.entries.forEach { assertEquals(it, converters.toFilmFormat(converters.fromFilmFormat(it))) }
        StopIncrement.entries.forEach { assertEquals(it, converters.toStopIncrement(converters.fromStopIncrement(it))) }
    }
}
