package org.better.urn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration

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
    modifier: Modifier
) {
    var jfxPanelRef by remember { mutableStateOf<JFXPanel?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(url) {
        onBufferingStateChanged(true)
        hasError = false
        try {
            val panel = JFXPanel()
            jfxPanelRef = panel
            Platform.runLater {
                try {
                    val media = Media(url)
                    val player = MediaPlayer(media)
                    mediaPlayerRef = player

                    val mediaView = MediaView(player)
                    mediaView.isPreserveRatio = true

                    val root = Group(mediaView)
                    val scene = Scene(root, javafx.scene.paint.Color.BLACK)
                    panel.scene = scene

                    scene.widthProperty().addListener { _, _, newW ->
                        mediaView.fitWidth = newW.toDouble()
                    }
                    scene.heightProperty().addListener { _, _, newH ->
                        mediaView.fitHeight = newH.toDouble()
                    }

                    player.setOnReady {
                        isInitialized = true
                        onBufferingStateChanged(false)
                        val duration = player.totalDuration.toMillis().toLong().coerceAtLeast(0L)
                        onProgressUpdate(0L, duration)
                        player.volume = volume.toDouble()
                        player.rate = playbackSpeed.toDouble()
                        if (isPlaying) {
                            player.play()
                        }
                    }

                    player.currentTimeProperty().addListener { _, _, newTime ->
                        val pos = newTime.toMillis().toLong().coerceAtLeast(0L)
                        val dur = player.totalDuration.toMillis().toLong().coerceAtLeast(0L)
                        onProgressUpdate(pos, dur)
                    }

                    player.statusProperty().addListener { _, _, newStatus ->
                        val buffering = newStatus == MediaPlayer.Status.STALLED
                        onBufferingStateChanged(buffering)
                    }

                    player.setOnEndOfMedia {
                        val dur = player.totalDuration.toMillis().toLong().coerceAtLeast(0L)
                        onProgressUpdate(dur, dur)
                        onPlaybackEnded()
                    }

                    player.setOnError {
                        hasError = true
                        onBufferingStateChanged(false)
                        onError(player.error?.message ?: "Playback error in JavaFX media engine")
                    }
                } catch (e: Exception) {
                    hasError = true
                    onBufferingStateChanged(false)
                    onError(e.message ?: "Failed to initialize JavaFX video player")
                }
            }
        } catch (e: Exception) {
            hasError = true
            onBufferingStateChanged(false)
            onError(e.message ?: "JavaFX environment error")
        }
    }

    LaunchedEffect(isPlaying, isInitialized) {
        val player = mediaPlayerRef
        if (player != null && isInitialized) {
            Platform.runLater {
                try {
                    if (isPlaying) {
                        player.play()
                    } else {
                        player.pause()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(volume, isInitialized) {
        val player = mediaPlayerRef
        if (player != null && isInitialized) {
            Platform.runLater {
                try {
                    player.volume = volume.toDouble().coerceIn(0.0, 1.0)
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(playbackSpeed, isInitialized) {
        val player = mediaPlayerRef
        if (player != null && isInitialized) {
            Platform.runLater {
                try {
                    player.rate = playbackSpeed.toDouble()
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(seekToMs, isInitialized) {
        val target = seekToMs
        val player = mediaPlayerRef
        if (target != null && player != null && isInitialized) {
            Platform.runLater {
                try {
                    player.seek(Duration.millis(target.toDouble()))
                    onSeekCompleted()
                } catch (_: Exception) {
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayerRef?.let { mp ->
                Platform.runLater {
                    try {
                        mp.stop()
                        mp.dispose()
                    } catch (_: Exception) {
                    }
                }
            }
            mediaPlayerRef = null
            jfxPanelRef = null
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val panel = jfxPanelRef
        if (panel != null && !hasError) {
            SwingPanel(
                factory = { panel },
                modifier = Modifier.fillMaxSize()
            )
        } else if (hasError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Lecture vidéo Desktop",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ouvrez la vidéo dans votre navigateur ou lecteur externe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(url)
                        } catch (_: Exception) {
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir dans le navigateur / lecteur externe")
                }
            }
        }
    }
}
