package org.better.urn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.better.urn.data.ViewableFile
import org.better.urn.ui.navigation.BackHandler

/**
 * Material 3 fullscreen image viewer supporting desktop and mobile interactions.
 */
@Composable
fun ImageViewerOverlay(
    file: ViewableFile,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadFile: ((ViewableFile) -> Unit)? = null,
) {
    BackHandler(enabled = true) {
        onClose()
    }

    val uriHandler = LocalUriHandler.current
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore if focus is not obtainable
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
                    when (keyEvent.key) {
                        Key.Escape -> {
                            onClose()
                            true
                        }
                        Key.Equals, Key.Plus, Key.DirectionUp -> {
                            zoom = (zoom + 0.10f).coerceAtMost(5f)
                            true
                        }
                        Key.Minus, Key.DirectionDown -> {
                            zoom = (zoom - 0.10f).coerceAtLeast(0.25f)
                            if (zoom <= 1f) pan = Offset.Zero
                            true
                        }
                        Key.Zero, Key.Backspace -> {
                            zoom = 1f
                            pan = Offset.Zero
                            rotationAngle = 0f
                            true
                        }
                        Key.L -> {
                            rotationAngle = ((rotationAngle - 90f) + 360f) % 360f
                            true
                        }
                        Key.R -> {
                            rotationAngle = (rotationAngle + 90f) % 360f
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Main transformable image canvas
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Desktop mouse scroll wheel zoom
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.firstOrNull()
                                if (change != null) {
                                    val scrollDelta = change.scrollDelta.y
                                    val zoomFactor = if (scrollDelta < 0) 1.05f else 0.95f
                                    val newZoom = (zoom * zoomFactor).coerceIn(0.25f, 5f)
                                    zoom = newZoom
                                    if (newZoom <= 1f) pan = Offset.Zero
                                    change.consume()
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoom > 1.2f) {
                                zoom = 1f
                                pan = Offset.Zero
                            } else {
                                zoom = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, panAmount, zoomAmount, rotationChange ->
                        val newZoom = (zoom * zoomAmount).coerceIn(0.25f, 5f)
                        zoom = newZoom
                        rotationAngle += rotationChange
                        if (newZoom > 1f) {
                            pan += panAmount
                        } else {
                            pan = Offset.Zero
                        }
                    }
                }
        ) {
            val imageResource: Any = remember(file.url) {
                if (file.url.startsWith("http://") || file.url.startsWith("https://")) {
                    file.url
                } else {
                    val cleanPath = file.url.removePrefix("file:")
                    val localFile = java.io.File(cleanPath)
                    if (localFile.exists()) localFile else file.url
                }
            }
            KamelImage(
                resource = asyncPainterResource(data = imageResource),
                contentDescription = file.title,
                contentScale = ContentScale.Fit,

                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = pan.x
                        translationY = pan.y
                        rotationZ = rotationAngle
                    },
                onLoading = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onFailure = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BrokenImage,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Impossible de charger l'image",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
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
            )
        }

        // Top Bar
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
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
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
                    }
                ) {
                    Icon(
                        imageVector = if (onDownloadFile != null) Icons.Rounded.Download else Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = if (onDownloadFile != null) "Télécharger le fichier" else "Ouvrir dans un navigateur externe",
                        tint = Color.White
                    )
                }
            }
        }

        // Floating Bottom Control Bar
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
            tonalElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        zoom = (zoom - 0.10f).coerceAtLeast(0.25f)
                        if (zoom <= 1f) pan = Offset.Zero
                    },
                    enabled = zoom > 0.25f
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomOut,
                        contentDescription = "Dézoomer",
                        tint = if (zoom > 0.25f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                Text(
                    text = "${(zoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = {
                        zoom = (zoom + 0.10f).coerceAtMost(5f)
                    },
                    enabled = zoom < 5f
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = "Zoomer",
                        tint = if (zoom < 5f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                IconButton(
                    onClick = {
                        rotationAngle = (rotationAngle - 90f + 360f) % 360f
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                        contentDescription = "Pivoter de 90° vers la gauche",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        rotationAngle = (rotationAngle + 90f) % 360f
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                        contentDescription = "Pivoter de 90° vers la droite",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        zoom = 1f
                        pan = Offset.Zero
                        rotationAngle = 0f
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Réinitialiser la vue",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
