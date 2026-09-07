package org.better.urn.ui.universitice.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.better.urn.data.CourseModule
import org.better.urn.data.DownloadState
import org.better.urn.data.DownloadStatus
import org.better.urn.data.ViewableFile
import org.better.urn.data.toViewableFile

@Composable
fun CourseModuleItem(
    module: CourseModule,
    token: String,
    modifier: Modifier = Modifier,
    onOpenFile: ((ViewableFile) -> Unit)? = null,
    onDownloadFile: ((ViewableFile) -> Unit)? = null,
    onOpenFolder: ((CourseModule) -> Unit)? = null,
    downloadState: DownloadState? = null
) {
    val uriHandler = LocalUriHandler.current
    val primaryUrl = remember(module, token) { module.getPrimaryUrl(token) }
    val viewableFile = remember(module, token) { module.toViewableFile(token) }

    if (module.modname == "label") {
        LabelModuleItem(
            name = module.name,
            description = module.description,
            modifier = modifier
        )
        return
    }

    val firstContent = module.contents?.firstOrNull()
    val icon = remember(module.modname, firstContent?.mimetype) {
        getModuleIcon(module.modname, firstContent?.mimetype)
    }

    val subtitle = remember(module.modname, firstContent?.filesize) {
        val fileSize = firstContent?.getFormattedFileSize()
        buildString {
            if (!fileSize.isNullOrBlank()) {
                append(fileSize)
            }
            val typeLabel = getModuleTypeLabel(module.modname)
            if (typeLabel.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(typeLabel)
            }
        }
    }

    val cleanedName = module.name
    val cleanedDescription = module.description?.takeIf { it.isNotBlank() }
    val hasSupporting = subtitle.isNotBlank() || cleanedDescription != null

    val isViewableInApp = viewableFile?.isViewableInApp == true
    val isDownloadable = viewableFile != null &&
            !isViewableInApp &&
            module.modname != "forum" &&
            module.modname != "url" &&
            module.modname != "page" &&
            module.modname != "quiz"

    val isDownloadingThisFile = viewableFile != null &&
            downloadState?.file?.id == viewableFile.id &&
            downloadState.status == DownloadStatus.DOWNLOADING

    ListItem(
        headlineContent = {
            Text(
                text = cleanedName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = if (hasSupporting) {
            {
                Column {
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (cleanedDescription != null) {
                        if (subtitle.isNotBlank()) Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cleanedDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else null,
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        trailingContent = {
            if (isDownloadingThisFile) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp)
                ) {
                    val progress = downloadState?.progress ?: 0f
                    if (progress > 0f) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else if (module.modname == "folder" || primaryUrl != null) {
                val trailingIcon = when {
                    module.modname == "folder" -> Icons.AutoMirrored.Rounded.ArrowForward
                    isViewableInApp -> Icons.Rounded.Visibility
                    isDownloadable -> Icons.Rounded.Download
                    else -> Icons.AutoMirrored.Rounded.ArrowForward
                }
                val contentDesc = when {
                    module.modname == "folder" -> "Ouvrir le dossier"
                    isViewableInApp -> "Aperçu in-app"
                    isDownloadable -> "Télécharger le fichier"
                    else -> "Ouvrir"
                }

                Icon(
                    imageVector = trailingIcon,
                    contentDescription = contentDesc,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (module.modname == "folder" && onOpenFolder != null) {
                    Modifier.clickable { onOpenFolder(module) }
                } else if (primaryUrl != null) {
                    Modifier.clickable {
                        if (viewableFile != null && isViewableInApp && onOpenFile != null) {
                            onOpenFile(viewableFile)
                        } else if (viewableFile != null && isDownloadable && onDownloadFile != null) {
                            onDownloadFile(viewableFile)
                        } else {
                            try {
                                uriHandler.openUri(primaryUrl)
                            } catch (_: Exception) {
                                // Fallback gracefully if URL handler is not supported
                            }
                        }
                    }
                } else Modifier
            )
    )
}

@Composable
private fun LabelModuleItem(
    name: String,
    description: String?,
    modifier: Modifier = Modifier
) {
    val cleanedText = if (!description.isNullOrBlank()) description else name
    if (cleanedText.isBlank()) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(
            text = cleanedText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private fun getModuleIcon(modName: String, mimeType: String?): ImageVector {
    return when (modName) {
        "resource" -> {
            when {
                mimeType?.contains("image") == true -> Icons.Rounded.Image
                mimeType?.contains("video") == true -> Icons.Rounded.PlayCircle
                mimeType?.contains("pdf") == true -> Icons.Rounded.PictureAsPdf
                else -> Icons.Rounded.Description
            }
        }
        "url" -> Icons.Rounded.Link
        "forum" -> Icons.Rounded.Forum
        "assign" -> Icons.Rounded.Assignment
        "folder" -> Icons.Rounded.Folder
        "page" -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }
}

private fun getModuleTypeLabel(modName: String): String {
    return when (modName) {
        "resource" -> "Fichier"
        "url" -> "Lien Web"
        "forum" -> "Forum"
        "assign" -> "Devoir"
        "quiz" -> "Test / Quiz"
        "folder" -> "Dossier"
        "page" -> "Page"
        else -> ""
    }
}
