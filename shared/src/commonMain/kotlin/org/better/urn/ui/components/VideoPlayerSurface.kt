package org.better.urn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific video playback surface.
 */
@Composable
expect fun VideoPlayerSurface(
    url: String,
    isPlaying: Boolean,
    volume: Float,
    playbackSpeed: Float,
    seekToMs: Long?,
    onSeekCompleted: () -> Unit,
    onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit,
    onBufferingStateChanged: (isBuffering: Boolean) -> Unit,
    onPlaybackEnded: () -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier = Modifier
)
