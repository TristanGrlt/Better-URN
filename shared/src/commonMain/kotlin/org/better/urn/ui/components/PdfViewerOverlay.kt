package org.better.urn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.better.urn.data.ViewableFile
import org.better.urn.ui.navigation.BackHandler

/**
 * Material 3 fullscreen PDF viewer overlay with page navigation, zoom, rotation,
 * jump-to-page input, and thumbnail grid selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerOverlay(
    file: ViewableFile,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onClose()
    }

    val uriHandler = LocalUriHandler.current
    val state = rememberPdfViewerState()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isJumpPageDialogOpen by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
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
                            if (state.isGridVisible) {
                                state.toggleGrid()
                            } else {
                                onClose()
                            }
                            true
                        }
                        Key.DirectionLeft, Key.PageUp -> {
                            state.previousPage()
                            true
                        }
                        Key.DirectionRight, Key.PageDown -> {
                            state.nextPage()
                            true
                        }
                        Key.MoveHome -> {
                            state.goToPage(1)
                            true
                        }
                        Key.MoveEnd -> {
                            state.goToPage(state.pageCount)
                            true
                        }
                        Key.Equals, Key.Plus, Key.DirectionUp -> {
                            state.zoomIn()
                            true
                        }
                        Key.Minus, Key.DirectionDown -> {
                            state.zoomOut()
                            true
                        }
                        Key.Zero, Key.Backspace -> {
                            state.resetView()
                            true
                        }
                        Key.L -> {
                            state.rotateCounterClockwise()
                            true
                        }
                        Key.R -> {
                            state.rotateClockwise()
                            true
                        }
                        Key.F -> {
                            state.toggleFitMode()
                            true
                        }
                        Key.G -> {
                            state.toggleGrid()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Main PDF Render Surface
        PdfRenderSurface(
            url = file.url,
            state = state,
            onLoadingStateChanged = { loading -> isLoading = loading },
            onError = { err -> errorMessage = err },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Overlay
        if (isLoading && errorMessage == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    tonalElevation = 6.dp
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Error Overlay
        if (errorMessage != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Impossible de charger le document PDF",
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
                        try {
                            uriHandler.openUri(file.url)
                        } catch (_: Exception) {
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Ouvrir dans un navigateur externe",
                        tint = Color.White
                    )
                }
            }
        }

        // Floating Bottom Control Bar
        var isOverflowMenuExpanded by remember { mutableStateOf(false) }

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
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // Page Prev Button
                IconButton(
                    onClick = { state.previousPage() },
                    enabled = state.currentPage > 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                        contentDescription = "Page précédente",
                        tint = if (state.currentPage > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Page Number Button / Jump Dialog Trigger (without the word "Page")
                TextButton(
                    onClick = { isJumpPageDialogOpen = true },
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (state.pageCount > 0) "${state.currentPage} / ${state.pageCount}" else "${state.currentPage}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Page Next Button
                IconButton(
                    onClick = { state.nextPage() },
                    enabled = state.pageCount == 0 || state.currentPage < state.pageCount
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "Page suivante",
                        tint = if (state.pageCount == 0 || state.currentPage < state.pageCount) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                // Zoom Out
                IconButton(
                    onClick = { state.zoomOut() },
                    enabled = state.zoom > PdfViewerState.MIN_ZOOM
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomOut,
                        contentDescription = "Dézoomer",
                        tint = if (state.zoom > PdfViewerState.MIN_ZOOM) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                Text(
                    text = "${(state.zoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                // Zoom In
                IconButton(
                    onClick = { state.zoomIn() },
                    enabled = state.zoom < PdfViewerState.MAX_ZOOM
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = "Zoomer",
                        tint = if (state.zoom < PdfViewerState.MAX_ZOOM) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                // More Options / Overflow Menu
                Box {
                    IconButton(onClick = { isOverflowMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Plus d'options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = isOverflowMenuExpanded,
                        onDismissRequest = { isOverflowMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (state.fitMode == PdfFitMode.FIT_WIDTH) "Mode pleine page" else "Ajuster la largeur"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (state.fitMode == PdfFitMode.FIT_WIDTH) Icons.Rounded.FitScreen else Icons.Rounded.AspectRatio,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                state.toggleFitMode()
                                isOverflowMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Afficher les miniatures") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                state.toggleGrid()
                                isOverflowMenuExpanded = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Pivoter vers la gauche") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                state.rotateCounterClockwise()
                                isOverflowMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pivoter vers la droite") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                state.rotateClockwise()
                                isOverflowMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Réinitialiser la vue") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                state.resetView()
                                isOverflowMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Jump to Page Dialog
        if (isJumpPageDialogOpen) {
            var inputPageText by remember { mutableStateOf(state.currentPage.toString()) }

            AlertDialog(
                onDismissRequest = { isJumpPageDialogOpen = false },
                title = {
                    Text(
                        text = "Aller à la page",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = inputPageText,
                            onValueChange = { inputPageText = it.filter { char -> char.isDigit() } },
                            label = { Text("Numéro de page") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val pageNum = inputPageText.toIntOrNull()
                                    if (pageNum != null) {
                                        state.goToPage(pageNum)
                                    }
                                    isJumpPageDialogOpen = false
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.pageCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Entrez un numéro entre 1 et ${state.pageCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pageNum = inputPageText.toIntOrNull()
                            if (pageNum != null) {
                                state.goToPage(pageNum)
                            }
                            isJumpPageDialogOpen = false
                        }
                    ) {
                        Text("Accéder")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isJumpPageDialogOpen = false }) {
                        Text("Annuler")
                    }
                }
            )
        }

        // Page Grid Overlay Drawer / Modal
        if (state.isGridVisible && state.pageCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { state.toggleGrid() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight(0.7f)
                        .padding(16.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Toutes les pages (${state.pageCount})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { state.toggleGrid() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Fermer la grille"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 90.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.pageCount) { index ->
                                val pageNum = index + 1
                                val isSelected = pageNum == state.currentPage

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .aspectRatio(0.75f)
                                        .clickable {
                                            state.goToPage(pageNum)
                                            state.toggleGrid()
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "$pageNum",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
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
}
