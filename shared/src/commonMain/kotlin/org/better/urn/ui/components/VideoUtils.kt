package org.better.urn.ui.components

object VideoUtils {
    val SUPPORTED_PLAYBACK_SPEEDS = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)

    /**
     * Formats milliseconds into a standard HH:MM:SS or MM:SS time string.
     */
    fun formatDurationMs(millis: Long): String {
        if (millis <= 0) return "00:00"
        val totalSeconds = millis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
    }

    /**
     * Calculates next playback speed in circular order.
     */
    fun getNextPlaybackSpeed(currentSpeed: Float): Float {
        val currentIndex = SUPPORTED_PLAYBACK_SPEEDS.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.05f }
        return if ((currentIndex == -1) || (currentIndex == SUPPORTED_PLAYBACK_SPEEDS.lastIndex)) {
            SUPPORTED_PLAYBACK_SPEEDS.first()
        } else {
            SUPPORTED_PLAYBACK_SPEEDS[currentIndex + 1]
        }
    }
}
