package org.better.urn.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoUtilsTest {

    @Test
    fun testFormatDurationMsWithZeroOrNegative() {
        assertEquals("00:00", VideoUtils.formatDurationMs(0))
        assertEquals("00:00", VideoUtils.formatDurationMs(-1000))
    }

    @Test
    fun testFormatDurationMsShortDuration() {
        assertEquals("00:05", VideoUtils.formatDurationMs(5000))
        assertEquals("01:05", VideoUtils.formatDurationMs(65000))
        assertEquals("59:59", VideoUtils.formatDurationMs(3599000))
    }

    @Test
    fun testFormatDurationMsLongDurationWithHours() {
        assertEquals("01:00:00", VideoUtils.formatDurationMs(3600000))
        assertEquals("02:15:30", VideoUtils.formatDurationMs(8130000))
    }

    @Test
    fun testNextPlaybackSpeed() {
        assertEquals(1.0f, VideoUtils.getNextPlaybackSpeed(0.5f))
        assertEquals(1.25f, VideoUtils.getNextPlaybackSpeed(1.0f))
        assertEquals(1.5f, VideoUtils.getNextPlaybackSpeed(1.25f))
        assertEquals(2.0f, VideoUtils.getNextPlaybackSpeed(1.5f))
        assertEquals(0.5f, VideoUtils.getNextPlaybackSpeed(2.0f))
        assertEquals(0.5f, VideoUtils.getNextPlaybackSpeed(3.0f))
    }
}
