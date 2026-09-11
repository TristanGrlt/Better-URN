package org.better.urn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.better.urn.data.ViewableFile
import org.better.urn.ui.navigation.BackHandler

/**
 * Material 3 fullscreen video player overlay supporting mobile and desktop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerOverlay(
    file: ViewableFile,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadFile: ((ViewableFile) -> Unit)? = null,
) {
    BackHandler(enabled = true) {
        onClose()
    }

    val uriHandler = LocalUriHandler.current
    var isPlaying by rememberSaveable(inputs = arrayOf(file.id)) { mutableStateOf(value = true) }
    var currentPosMs by rememberSaveable(inputs = arrayOf(file.id)) { mutableLongStateOf(0L) }
    var durationMs by rememberSaveable(inputs = arrayOf(file.id)) { mutableLongStateOf(0L) }
    var volume by rememberSaveable(inputs = arrayOf(file.id)) { mutableFloatStateOf(1f) }
    var lastVolume by rememberSaveable(inputs = arrayOf(file.id)) { mutableFloatStateOf(1f) }
    var playbackSpeed by rememberSaveable(inputs = arrayOf(file.id)) { mutableFloatStateOf(1f) }
    var isFullscreen by rememberSaveable(inputs = arrayOf(file.id)) { mutableStateOf(false) }

    var seekTargetMs by remember { mutableStateOf<Long?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var controlsVisible by remember { mutableStateOf(true) }
    var isSpeedMenuExpanded by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    PlatformVideoFullscreenEffect(
        isFullscreen = isFullscreen,
        controlsVisible = controlsVisible
    )

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(kotlin.time.Duration.parse("3500ms"))
            controlsVisible = false
        }
    }

    val resetAutoHide = {
        controlsVisible = true
    }

    val togglePlayPause = {
        resetAutoHide()
        if ((durationMs > 0) && (currentPosMs >= durationMs - 500L)) {
            seekTargetMs = 0L
            isPlaying = true
        } else {
            isPlaying = !isPlaying
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    resetAutoHide()
                    when (keyEvent.key) {
                        Key.Escape -> {
                            if (isFullscreen) {
                                isFullscreen = false
                            } else {
                                onClose()
                            }
                            true
                        }
                        Key.Spacebar, Key.K -> {
                            togglePlayPause()
                            true
                        }
                        Key.F -> {
                            isFullscreen = !isFullscreen
                            true
                        }
                        Key.DirectionLeft, Key.J -> {
                            val newPos = (currentPosMs - 10000L).coerceAtLeast(0L)
                            seekTargetMs = newPos
                            true
                        }
                        Key.DirectionRight, Key.L -> {
                            val newPos = (currentPosMs + 10000L).coerceAtMost(durationMs)
                            seekTargetMs = newPos
                            true
                        }
                        Key.DirectionUp -> {
                            volume = (volume + 0.1f).coerceAtMost(1f)
                            true
                        }
                        Key.DirectionDown -> {
                            volume = (volume - 0.1f).coerceAtLeast(0f)
                            true
                        }
                        Key.M -> {
                            if (volume > 0f) {
                                lastVolume = volume
                                volume = 0f
                            } else {
                                volume = if (lastVolume > 0f) lastVolume else 1f
                            }
                            true
                        }
                        Key.Zero, Key.Backspace -> {
                            seekTargetMs = 0L
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.type == PointerEventType.Move) {
                            resetAutoHide()
                        }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // Video Surface
        VideoPlayerSurface(
            url = file.url,
            isPlaying = isPlaying,
            volume = volume,
            playbackSpeed = playbackSpeed,
            seekToMs = seekTargetMs,
            onSeekCompleted = { seekTargetMs = null },
            onProgressUpdate = { pos, dur ->
                currentPosMs = pos
                durationMs = dur
            },
            onBufferingStateChanged = { buffering ->
                isBuffering = buffering
            },
            onPlaybackEnded = {
                isPlaying = false
                currentPosMs = durationMs
            },
            onError = { err ->
                errorMessage = err
                isBuffering = false
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading/Buffering morphing indicator
        if (isBuffering && errorMessage == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    tonalElevation = 6.dp
                ) {
                    Box(modifier = Modifier.padding(20.dp)) {
                        ContainedLoadingIndicator(
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }
        }

        // Error overlay
        if (errorMessage != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Impossible de lire la vidéo",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(file.url)
                            } catch (_: Exception) {
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ouvrir dans le navigateur")
                    }
                }
            }
        }

        // Animated Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar (hidden in Fullscreen mode for immersive playback)
                if (!isFullscreen) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            IconButton(
                                onClick = onClose,
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Retour"
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!file.formattedFileSize.isNullOrBlank()) {
                                    Text(
                                        text = file.formattedFileSize,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (onDownloadFile != null) {
                                        onDownloadFile(file)
                                    } else {
                                        try {
                                            uriHandler.openUri(file.url)
                                        } catch (_: Exception) {
                                        }
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                            ) {
                                Icon(
                                    imageVector = if (onDownloadFile != null) Icons.Rounded.Download else Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = if (onDownloadFile != null) "Télécharger le fichier" else "Ouvrir dans un navigateur externe"
                                )
                            }
                        }
                    }
                }

                // Center Play / Pause Big Hero Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                            contentColor = Color.White
                        ) {
                            IconButton(
                                onClick = {
                                    resetAutoHide()
                                    seekTargetMs = (currentPosMs - 10000L).coerceAtLeast(0L)
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay10,
                                    contentDescription = "Reculer de 10s",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = togglePlayPause,
                            shape = RoundedCornerShape(22.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Lecture",
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                            contentColor = Color.White
                        ) {
                            IconButton(
                                onClick = {
                                    resetAutoHide()
                                    seekTargetMs = (currentPosMs + 10000L).coerceAtMost(durationMs)
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forward10,
                                    contentDescription = "Avancer de 10s",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Floating Bottom Controls Bar
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth(0.95f)
                    ) {
                        // Slider Scrub Control
                        Slider(
                            value = if (durationMs > 0) currentPosMs.toFloat() else 0f,
                            onValueChange = { newPos ->
                                resetAutoHide()
                                seekTargetMs = newPos.toLong()
                            },
                            valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Bottom Bar Play/Pause Button
                                FilledIconButton(
                                    onClick = togglePlayPause,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Lecture",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Duration Label
                                Text(
                                    text = "${VideoUtils.formatDurationMs(currentPosMs)} / ${VideoUtils.formatDurationMs(durationMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Volume Control
                                IconButton(
                                    onClick = {
                                        resetAutoHide()
                                        if (volume > 0f) {
                                            lastVolume = volume
                                            volume = 0f
                                        } else {
                                            volume = if (lastVolume > 0f) lastVolume else 1f
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (volume > 0f) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                        contentDescription = if (volume > 0f) "Couper le son" else "Activer le son",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Playback Speed Menu
                                Box {
                                    TextButton(
                                        onClick = {
                                            resetAutoHide()
                                            isSpeedMenuExpanded = true
                                        }
                                    ) {
                                        Text(
                                            text = "${playbackSpeed}x",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isSpeedMenuExpanded,
                                        onDismissRequest = { isSpeedMenuExpanded = false }
                                    ) {
                                        VideoUtils.SUPPORTED_PLAYBACK_SPEEDS.forEach { speed ->
                                            DropdownMenuItem(
                                                text = { Text("${speed}x") },
                                                onClick = {
                                                    playbackSpeed = speed
                                                    isSpeedMenuExpanded = false
                                                    resetAutoHide()
                                                }
                                            )
                                        }
                                    }
                                }

                                // Reset / Restart Video
                                IconButton(
                                    onClick = {
                                        resetAutoHide()
                                        seekTargetMs = 0L
                                        isPlaying = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.RestartAlt,
                                        contentDescription = "Recommencer la vidéo",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Fullscreen Toggle Button
                                IconButton(
                                    onClick = {
                                        resetAutoHide()
                                        isFullscreen = !isFullscreen
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                        contentDescription = if (isFullscreen) "Quitter le plein écran" else "Plein écran",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
