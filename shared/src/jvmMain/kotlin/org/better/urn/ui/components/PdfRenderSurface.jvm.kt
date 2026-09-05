package org.better.urn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import java.net.URL

@Composable
actual fun PdfRenderSurface(
    url: String,
    state: PdfViewerState,
    onLoadingStateChanged: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier
) {
    var pan by remember { mutableStateOf(Offset.Zero) }
    var pdfDocument by remember { mutableStateOf<PDDocument?>(null) }
    var pdfRenderer by remember { mutableStateOf<PDFRenderer?>(null) }
    val pageBitmaps = remember { mutableStateMapOf<Int, ImageBitmap>() }

    val lazyListState = rememberLazyListState()

    // Sync current page state with lazy list scroll
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstIndex ->
            state.setCurrentPageFromScroll(firstIndex + 1)
        }
    }

    // Scroll list when state.currentPage changes programmatically
    LaunchedEffect(state.currentPage) {
        if (state.pageCount > 0) {
            val targetIndex = (state.currentPage - 1).coerceIn(0, state.pageCount - 1)
            if (lazyListState.firstVisibleItemIndex != targetIndex) {
                lazyListState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Load PDF file
    LaunchedEffect(url) {
        onLoadingStateChanged(true)
        onError(null)

        try {
            val pdfFile = withContext(Dispatchers.IO) {
                val userHome = System.getProperty("user.home") ?: "."
                val cacheDir = File(userHome, ".betterurn/pdf_cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val safeFileName = url.hashCode().toString() + ".pdf"
                val file = File(cacheDir, safeFileName)
                if (!file.exists() || file.length() == 0L) {
                    val connection = URL(url).openConnection()
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (BetterURN)")
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.getInputStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                file
            }

            val doc = withContext(Dispatchers.IO) { Loader.loadPDF(pdfFile) }
            val renderer = PDFRenderer(doc)
            pdfDocument = doc
            pdfRenderer = renderer

            pageBitmaps.clear()
            state.updatePageCount(doc.numberOfPages)
            onLoadingStateChanged(false)
        } catch (e: Exception) {
            onLoadingStateChanged(false)
            onError(e.message ?: "Impossible de charger le document PDF")
        }
    }

    DisposableEffect(url) {
        onDispose {
            pageBitmaps.clear()
            try {
                pdfDocument?.close()
            } catch (_: Exception) {
            }
        }
    }

    // Clear bitmap cache on scale or mode change
    LaunchedEffect(state.fitMode) {
        pageBitmaps.clear()
    }

    // Reset pan when page changes
    LaunchedEffect(state.currentPage) {
        if (state.zoom <= 1f) pan = Offset.Zero
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.type == PointerEventType.Scroll) {
                            val isCtrlOrMeta = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                            if (isCtrlOrMeta) {
                                val change = event.changes.firstOrNull()
                                if (change != null) {
                                    val scrollDelta = change.scrollDelta.y
                                    if (scrollDelta < 0) {
                                        state.zoomIn()
                                    } else {
                                        state.zoomOut()
                                        if (state.zoom <= 1f) pan = Offset.Zero
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (state.zoom > 1.2f) {
                            state.resetZoom()
                            pan = Offset.Zero
                        } else {
                            state.setZoomLevel(2.5f)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, panAmount, zoomAmount, _ ->
                    val newZoom = (state.zoom * zoomAmount).coerceIn(
                        PdfViewerState.MIN_ZOOM,
                        PdfViewerState.MAX_ZOOM
                    )
                    state.setZoomLevel(newZoom)
                    if (newZoom > 1f) {
                        pan += panAmount
                    } else {
                        pan = Offset.Zero
                    }
                }
            }
    ) {
        val renderer = pdfRenderer
        if (renderer != null && state.pageCount > 0) {
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 64.dp, bottom = 96.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = state.zoom
                        scaleY = state.zoom
                        translationX = pan.x
                        translationY = pan.y
                        rotationZ = state.rotationAngle
                    }
            ) {
                items(
                    count = state.pageCount,
                    key = { index -> "pdf_page_$index" }
                ) { pageIndex ->
                    var bitmap by remember(pageIndex, state.fitMode) { mutableStateOf(pageBitmaps[pageIndex]) }

                    LaunchedEffect(pageIndex, state.fitMode) {
                        if (bitmap == null) {
                            val rendered = withContext(Dispatchers.Default) {
                                synchronized(renderer) {
                                    val dpi = if (state.fitMode == PdfFitMode.FIT_WIDTH) 200f else 150f
                                    val bufferedImage = renderer.renderImageWithDPI(pageIndex, dpi)
                                    bufferedImage.toComposeImageBitmap()
                                }
                            }
                            bitmap = rendered
                            pageBitmaps[pageIndex] = rendered
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        val currentBmp = bitmap
                        if (currentBmp != null) {
                            Image(
                                bitmap = currentBmp,
                                contentDescription = "Page ${pageIndex + 1}",
                                contentScale = if (state.fitMode == PdfFitMode.FIT_WIDTH) ContentScale.FillWidth else ContentScale.Fit,
                                modifier = if (state.fitMode == PdfFitMode.FIT_WIDTH) {
                                    Modifier.fillMaxWidth()
                                } else {
                                    Modifier.wrapContentSize()
                                }
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
