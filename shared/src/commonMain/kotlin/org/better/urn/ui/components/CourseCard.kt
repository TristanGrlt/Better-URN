package org.better.urn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.better.urn.data.Course

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseCard(
    course: Course,
    token: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleHide: (() -> Unit)? = null,
    isHidden: Boolean = false,
) {
    val imageUrl = course.imageUrl ?: course.getImageUrl(token)
    val imageResource: Any? = remember(imageUrl) {
        val url = imageUrl ?: return@remember null
        if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            val cleanPath = url.removePrefix("file:")
            val file = java.io.File(cleanPath)
            if (file.exists()) file else null
        }
    }
    var showMenu by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    Box(modifier = modifier) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isHidden) {
                    if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isHidden) 1.dp else 2.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = if (isDark) 0.25f else 0.35f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .alpha(if (isHidden) 0.85f else 1f)
                .pointerInput(onToggleHide) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if ((event.type == PointerEventType.Press) && event.buttons.isSecondaryPressed) {
                                if (onToggleHide != null) {
                                    showMenu = true
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (onToggleHide != null) {
                            showMenu = true
                        }
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize()
            ) {
                if (imageResource != null) {
                    KamelImage(
                        resource = asyncPainterResource(data = imageResource),
                        contentDescription = "Image du cours",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp),

                        onLoading = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(40.dp)
                            ) {
                                LoadingIndicator(modifier = Modifier.size(24.dp))
                            }
                        },
                        onFailure = {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.School,
                                        contentDescription = "Cours",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    )
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = "Cours sans image",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = course.fullname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.shortname,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onToggleHide != null) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(if (isHidden) "Afficher le cours" else "Cacher le cours")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        onToggleHide()
                    }
                )
            }
        }
    }
}
