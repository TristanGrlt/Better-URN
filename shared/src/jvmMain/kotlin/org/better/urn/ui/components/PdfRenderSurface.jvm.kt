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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    var pdfDocument by remember { mutableStateOf<PDDocument?>(null) }
    var pdfRenderer by remember { mutableStateOf<PDFRenderer?>(null) }

    val rendererMutex = remember { Mutex() }
    val lazyListState = rememberLazyListState()

    // Bounded LRU cache holding max 16 bitmaps to accommodate zoomed-out multi-page views safely
    val lruCache = remember {
        object : LinkedHashMap<Int, ImageBitmap>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ImageBitmap>?): Boolean {
                return size > 16
            }
        }
    }

    // Sync current page state with lazy list scroll
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstIndex ->
            state.setCurrentPageFromScroll(firstIndex + 1)
        }
    }

    // Scroll list when state.currentPage changes programmatically (prevents scroll feedback loop)
    LaunchedEffect(state.currentPage) {
        if (state.pageCount > 0 && !lazyListState.isScrollInProgress) {
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

            lruCache.clear()
            state.updatePageCount(doc.numberOfPages)
            onLoadingStateChanged(false)
        } catch (e: Exception) {
            onLoadingStateChanged(false)
            onError(e.message ?: "Impossible de charger le document PDF")
        }
    }

    DisposableEffect(url) {
        onDispose {
            lruCache.clear()
            try {
                pdfDocument?.close()
            } catch (_: Exception) {
            }
        }
    }

    // Clear bitmap cache on scale or mode change
    LaunchedEffect(state.fitMode) {
        lruCache.clear()
    }

    // Reset pan when zoom returns to <= 1f or page changes
    LaunchedEffect(state.currentPage) {
        if (state.zoom <= 1f) {
            panX = 0f
            panY = 0f
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                // Intercept scroll wheel with Ctrl/Meta in Initial pass for fine-grained progressive zoom
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Scroll) {
                            val isCtrlOrMeta = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                            if (isCtrlOrMeta) {
                                val change = event.changes.firstOrNull()
                                if (change != null) {
                                    val scrollDelta = change.scrollDelta.y
                                    val factor = if (scrollDelta < 0) 1.05f else 0.95f
                                    val newZoom = (state.zoom * factor).coerceIn(
                                        PdfViewerState.MIN_ZOOM,
                                        PdfViewerState.MAX_ZOOM
                                    )
                                    state.setZoomLevel(newZoom)
                                    if (newZoom <= 1f) {
                                        panX = 0f
                                        panY = 0f
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
                            panX = 0f
                            panY = 0f
                        } else {
                            state.setZoomLevel(2.5f)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, panAmount, zoomAmount, _ ->
                    if (zoomAmount != 1f) {
                        val newZoom = (state.zoom * zoomAmount).coerceIn(
                            PdfViewerState.MIN_ZOOM,
                            PdfViewerState.MAX_ZOOM
                        )
                        state.setZoomLevel(newZoom)
                    }
                    if (state.zoom > 1f) {
                        panX += panAmount.x
                        panY += panAmount.y
                    } else {
                        panX = 0f
                        panY = 0f
                    }
                }
            }
    ) {
        val renderer = pdfRenderer
        if (renderer != null && state.pageCount > 0) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val containerWidth = maxWidth
                val containerHeight = maxHeight

                LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 64.dp, bottom = 96.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = state.pageCount,
                        key = { index -> "pdf_page_$index" }
                    ) { pageIndex ->
                        var bitmap by remember(pageIndex, state.fitMode) { mutableStateOf<ImageBitmap?>(null) }

                        LaunchedEffect(pageIndex, state.fitMode) {
                            val cached = lruCache[pageIndex]
                            if (cached != null) {
                                bitmap = cached
                            } else {
                                val rendered = withContext(Dispatchers.Default) {
                                    rendererMutex.withLock {
                                        val existing = lruCache[pageIndex]
                                        if (existing != null) {
                                            return@withLock existing
                                        }

                                        val dpi = if (state.fitMode == PdfFitMode.FIT_WIDTH) 180f else 140f
                                        val bufferedImage = renderer.renderImageWithDPI(pageIndex, dpi)
                                        val composeBmp = bufferedImage.toComposeImageBitmap()
                                        lruCache[pageIndex] = composeBmp
                                        composeBmp
                                    }
                                }
                                bitmap = rendered
                            }
                        }

                        val currentBmp = bitmap
                        if (currentBmp != null) {
                            val bmpWidth = currentBmp.width.toFloat()
                            val bmpHeight = currentBmp.height.toFloat()
                            val aspectRatio = if (bmpWidth > 0f) bmpHeight / bmpWidth else 1.414f

                            val baseWidth = if (state.fitMode == PdfFitMode.FIT_WIDTH) {
                                (containerWidth - 32.dp).coerceAtLeast(100.dp)
                            } else {
                                val fitHeight = (containerHeight - 128.dp).coerceAtLeast(200.dp)
                                val widthFromHeight = fitHeight / aspectRatio
                                minOf(containerWidth - 32.dp, widthFromHeight).coerceAtLeast(100.dp)
                            }
                            val baseHeight = baseWidth * aspectRatio

                            val isRotated90 = (state.rotationAngle.toInt() % 180 != 0)
                            val layoutBaseWidth = if (isRotated90) baseHeight else baseWidth
                            val layoutBaseHeight = if (isRotated90) baseWidth else baseHeight

                            val scaledWidth = layoutBaseWidth * state.zoom
                            val scaledHeight = layoutBaseHeight * state.zoom

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .requiredSize(scaledWidth, scaledHeight)
                                        .graphicsLayer {
                                            if (state.zoom > 1f) {
                                                translationX = panX
                                                translationY = panY
                                            }
                                            rotationZ = state.rotationAngle
                                        }
                                ) {
                                    Image(
                                        bitmap = currentBmp,
                                        contentDescription = "Page ${pageIndex + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
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
