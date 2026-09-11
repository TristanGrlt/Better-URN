package org.better.urn.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
actual fun VideoPlayerSurface(
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
    modifier: Modifier,
) {
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPrepared by remember { mutableStateOf(value = false) }

    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                videoViewRef = this
                setVideoURI(Uri.parse(url))

                setOnPreparedListener { mp ->
                    mediaPlayerRef = mp
                    isPrepared = true
                    onBufferingStateChanged(false)

                    mp.isLooping = false
                    mp.setVolume(volume, volume)

                    try {
                        val params = mp.playbackParams
                        params.speed = playbackSpeed
                        mp.playbackParams = params
                    } catch (_: Exception) {
                    }

                    val duration = mp.duration.toLong().coerceAtLeast(0L)
                    val currentPos = mp.currentPosition.toLong().coerceAtLeast(0L)
                    onProgressUpdate(currentPos, duration)

                    if (isPlaying) {
                        start()
                    }
                }

                setOnInfoListener { _, what, _ ->
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                            onBufferingStateChanged(true)
                            true
                        }
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                            onBufferingStateChanged(false)
                            true
                        }
                        else -> false
                    }
                }

                setOnErrorListener { _, what, extra ->
                    onBufferingStateChanged(false)
                    onError("Video playback error code: $what ($extra)")
                    true
                }

                setOnCompletionListener {
                    val duration = duration.toLong().coerceAtLeast(0L)
                    onProgressUpdate(duration, duration)
                    onPlaybackEnded()
                }
            }
        },
        update = { _ ->
            if (isPrepared) {
                mediaPlayerRef?.let { mp ->
                    try {
                        mp.setVolume(volume, volume)
                        val params = mp.playbackParams
                        if (params.speed != playbackSpeed) {
                            params.speed = playbackSpeed
                            mp.playbackParams = params
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        },
        modifier = modifier
    )

    LaunchedEffect(isPlaying, isPrepared) {
        val vView = videoViewRef
        if ((vView != null) && isPrepared) {
            if (isPlaying) {
                if (!vView.isPlaying) {
                    vView.start()
                }
            } else {
                if (vView.isPlaying) {
                    vView.pause()
                }
            }
        }
    }

    LaunchedEffect(seekToMs, isPrepared) {
        val vView = videoViewRef
        if (seekToMs != null && (vView != null) && isPrepared) {
            vView.seekTo(seekToMs.toInt())
            onSeekCompleted()
        }
    }

    LaunchedEffect(isPlaying, isPrepared) {
        if (isPrepared) {
            while (isActive) {
                val vView = videoViewRef
                if (vView != null) {
                    val pos = vView.currentPosition.toLong().coerceAtLeast(0L)
                    val dur = vView.duration.toLong().coerceAtLeast(0L)
                    onProgressUpdate(pos, dur)
                }
                delay(kotlin.time.Duration.parse("250ms"))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
            mediaPlayerRef = null
            videoViewRef = null
        }
    }
}
