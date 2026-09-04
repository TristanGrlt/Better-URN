package org.better.urn.ui.universitice.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.better.urn.data.CourseModule

@Composable
fun CourseModuleItem(
    module: CourseModule,
    token: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val primaryUrl = remember(module, token) { module.getPrimaryUrl(token) }

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
            if (primaryUrl != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Ouvrir",
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
                if (primaryUrl != null) {
                    Modifier.clickable {
                        try {
                            uriHandler.openUri(primaryUrl)
                        } catch (_: Exception) {
                            // Fallback gracefully if URL handler is not supported
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
