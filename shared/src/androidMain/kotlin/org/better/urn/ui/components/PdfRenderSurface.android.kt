package org.better.urn.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.better.urn.data.AndroidContextProvider
import java.io.File
import java.net.URL

@Composable
actual fun PdfRenderSurface(
    url: String,
    state: PdfViewerState,
    onLoadingStateChanged: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier,
) {
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    val rendererMutex = remember { Mutex() }
    val lazyListState = rememberLazyListState()

    // Bounded LRU cache holding up to 16 page bitmaps to accommodate zoomed-out multi-page views safely
    val lruCache = remember { LruCache<Int, Bitmap>(16) }

    // Sync current page state with lazy list scroll
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstIndex ->
            state.setCurrentPageFromScroll(firstIndex + 1)
        }
    }

    // Scroll list when state.currentPage changes programmatically (prevents scroll feedback loop)
    LaunchedEffect(state.currentPage) {
        if ((state.pageCount > 0) && !lazyListState.isScrollInProgress) {
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
        val context = AndroidContextProvider.context
        if (context == null) {
            onLoadingStateChanged(false)
            onError("Context non disponible")
            return@LaunchedEffect
        }

        try {
            val pdfFile = withContext(Dispatchers.IO) {
                val cacheDir = File(context.cacheDir, "pdf_cache")
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

            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            fileDescriptor = pfd
            pdfRenderer = renderer

            lruCache.evictAll()
            state.updatePageCount(renderer.pageCount)
            onLoadingStateChanged(false)
        } catch (e: Exception) {
            onLoadingStateChanged(false)
            onError(e.message ?: "Impossible de charger le document PDF")
        }
    }

    DisposableEffect(url) {
        onDispose {
            lruCache.evictAll()
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    // Clear bitmap cache on scale or mode change
    LaunchedEffect(state.fitMode) {
        lruCache.evictAll()
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
                val density = LocalDensity.current
                val containerWidth = maxWidth
                val containerHeight = maxHeight
                val containerWidthPx = with(density) { containerWidth.toPx() }.toInt()

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

                        LaunchedEffect(pageIndex, state.fitMode, containerWidthPx) {
                            val cached = lruCache[pageIndex]
                            if (cached != null && !cached.isRecycled) {
                                bitmap = cached.asImageBitmap()
                            } else {
                                val rendered = withContext(Dispatchers.Default) {
                                    rendererMutex.withLock {
                                        // Double check cache inside lock
                                        val existing = lruCache[pageIndex]
                                        if (existing != null && !existing.isRecycled) {
                                            return@withLock existing.asImageBitmap()
                                        }

                                        val page = renderer.openPage(pageIndex)
                                        val pageAspect = page.height.toFloat() / page.width.toFloat()

                                        // Render page targeting exact screen container pixels (bounded to safe GPU texture max 2048px)
                                        val targetWidthPx = containerWidthPx.coerceIn(300, 2048)
                                        val targetHeightPx = (targetWidthPx * pageAspect).toInt().coerceIn(300, 2048)

                                        val bmp = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
                                        bmp.eraseColor(AndroidColor.WHITE)
                                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        page.close()

                                        lruCache.put(pageIndex, bmp)
                                        bmp.asImageBitmap()
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
