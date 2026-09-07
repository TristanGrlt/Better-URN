package org.better.urn.ui.universitice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.better.urn.data.BreadcrumbSegment
import org.better.urn.data.CourseModule
import org.better.urn.data.DownloadState
import org.better.urn.data.DownloadStatus
import org.better.urn.data.FolderFileItem
import org.better.urn.data.FolderSubfolderItem
import org.better.urn.data.FolderTreeContent
import org.better.urn.data.ViewableFile
import org.better.urn.data.ViewableFileType
import org.better.urn.ui.components.BetterUrnTopBar

@Composable
fun FolderDetailScreen(
    folderModule: CourseModule,
    treeContent: FolderTreeContent?,
    searchQuery: String,
    downloadState: DownloadState?,
    onBackClick: () -> Unit,
    onNavigateToSubfolder: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDownloadAllFiles: () -> Unit,
    onOpenFile: (ViewableFile) -> Unit,
    onDownloadFile: (ViewableFile) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BetterUrnTopBar(
            title = folderModule.name,
            onBackClick = onBackClick,
            onRefresh = onRefresh,
            isRefreshing = false,
            refreshContentDescription = "Actualiser le dossier"
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                FolderHeaderCard(
                    folderName = folderModule.name,
                    description = folderModule.description,
                    totalFilesCount = treeContent?.files?.size ?: 0,
                    onDownloadAllFiles = onDownloadAllFiles
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (treeContent != null && treeContent.breadcrumbs.size > 1) {
                    BreadcrumbNavigationBar(
                        breadcrumbs = treeContent.breadcrumbs,
                        currentPath = treeContent.currentPath,
                        onNavigateToPath = onNavigateToSubfolder
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                FolderSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (treeContent == null || (treeContent.subfolders.isEmpty() && treeContent.files.isEmpty())) {
                    FolderEmptyStateView(
                        searchQuery = searchQuery,
                        onClearSearch = { onSearchQueryChange("") }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (treeContent.subfolders.isNotEmpty()) {
                            item(key = "header_subfolders") {
                                Text(
                                    text = "Dossiers (${treeContent.subfolders.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }

                            items(
                                items = treeContent.subfolders,
                                key = { "subfolder_${it.fullPath}" }
                            ) { subfolder ->
                                SubfolderItemCard(
                                    subfolder = subfolder,
                                    onClick = { onNavigateToSubfolder(subfolder.fullPath) }
                                )
                            }
                        }

                        if (treeContent.files.isNotEmpty()) {
                            item(key = "header_files") {
                                val topPadding = if (treeContent.subfolders.isNotEmpty()) 12.dp else 4.dp
                                Text(
                                    text = "Fichiers (${treeContent.files.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = topPadding)
                                )
                            }

                            items(
                                items = treeContent.files,
                                key = { "file_${it.viewableFile.id}" }
                            ) { fileItem ->
                                FolderFileListItem(
                                    fileItem = fileItem,
                                    downloadState = downloadState,
                                    onOpenFile = onOpenFile,
                                    onDownloadFile = onDownloadFile
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderHeaderCard(
    folderName: String,
    description: String?,
    totalFilesCount: Int,
    onDownloadAllFiles: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (totalFilesCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalFilesCount fichier${if (totalFilesCount > 1) "s" else ""} disponible${if (totalFilesCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (totalFilesCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onDownloadAllFiles,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tout télécharger")
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbNavigationBar(
    breadcrumbs: List<BreadcrumbSegment>,
    currentPath: String,
    onNavigateToPath: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            breadcrumbs.forEachIndexed { index, segment ->
                val isLast = index == breadcrumbs.lastIndex
                val isSelected = segment.path == currentPath

                if (index > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(horizontal = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = !isLast) {
                        onNavigateToPath(segment.path)
                    }
                ) {
                    Text(
                        text = segment.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = CircleShape,
        color = if (isFocused) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Filtrer dans le dossier",
                modifier = Modifier.size(18.dp),
                tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Rechercher un fichier...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Effacer la recherche",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SubfolderItemCard(
    subfolder: FolderSubfolderItem,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subfolder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${subfolder.itemCount} élément${if (subfolder.itemCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FolderFileListItem(
    fileItem: FolderFileItem,
    downloadState: DownloadState?,
    onOpenFile: (ViewableFile) -> Unit,
    onDownloadFile: (ViewableFile) -> Unit
) {
    val file = fileItem.viewableFile
    val isDownloadingThisFile = downloadState?.file?.id == file.id &&
            downloadState.status == DownloadStatus.DOWNLOADING

    val isViewableInApp = file.isViewableInApp

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            },
            supportingContent = if (!file.formattedFileSize.isNullOrBlank()) {
                {
                    Text(
                        text = file.formattedFileSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            leadingContent = {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = file.getIcon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            trailingContent = {
                if (isDownloadingThisFile) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                        val progress = downloadState?.progress ?: 0f
                        if (progress > 0f) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    val trailingIcon = if (isViewableInApp) Icons.Rounded.Visibility else Icons.Rounded.Download
                    val contentDesc = if (isViewableInApp) "Aperçu in-app" else "Télécharger"
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = contentDesc,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    if (isViewableInApp) {
                        onOpenFile(file)
                    } else {
                        onDownloadFile(file)
                    }
                }
        )
    }
}

@Composable
private fun FolderEmptyStateView(
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aucun élément ne correspond à « $searchQuery »",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(onClick = onClearSearch) {
                    Text("Effacer la recherche")
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ce dossier ne contient aucun fichier ou sous-dossier.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun ViewableFile.getIcon(): ImageVector {
    return when (fileType) {
        ViewableFileType.IMAGE -> Icons.Rounded.Image
        ViewableFileType.PDF -> Icons.Rounded.PictureAsPdf
        ViewableFileType.VIDEO -> Icons.Rounded.PlayCircle
        ViewableFileType.DOCUMENT -> Icons.Rounded.Description
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }
}
