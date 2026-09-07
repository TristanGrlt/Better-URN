package org.better.urn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Modes for scaling the PDF document within the viewer surface.
 */
enum class PdfFitMode {
    FIT_PAGE,
    FIT_WIDTH
}

/**
 * Controller holding layout state, zoom factor, rotation, page tracking,
 * and display options for the PDF viewer.
 */
class PdfViewerState(
    initialPage: Int = 1,
    initialZoom: Float = 1f,
    initialRotation: Float = 0f,
    initialFitMode: PdfFitMode = PdfFitMode.FIT_PAGE
) {
    var currentPage by mutableIntStateOf(initialPage.coerceAtLeast(1))
        private set

    var pageCount by mutableIntStateOf(0)

    var zoom by mutableFloatStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set

    var rotationAngle by mutableFloatStateOf((initialRotation % 360f + 360f) % 360f)
        private set

    var fitMode by mutableStateOf(initialFitMode)
        private set

    var isGridVisible by mutableStateOf(false)

    /**
     * Updates the document's total page count and clamps current page if necessary.
     */
    fun updatePageCount(count: Int) {
        pageCount = count.coerceAtLeast(0)
        if (currentPage > pageCount && pageCount > 0) {
            currentPage = pageCount
        }
    }

    /**
     * Navigates to the next page if available.
     */
    fun nextPage(): Boolean {
        if (pageCount == 0 || currentPage < pageCount) {
            currentPage++
            return true
        }
        return false
    }

    /**
     * Navigates to the previous page if available.
     */
    fun previousPage(): Boolean {
        if (currentPage > 1) {
            currentPage--
            return true
        }
        return false
    }

    /**
     * Updates current page index from user scrolling without triggering programmatic scroll callbacks.
     */
    fun setCurrentPageFromScroll(page: Int) {
        val target = if (pageCount > 0) page.coerceIn(1, pageCount) else page.coerceAtLeast(1)
        currentPage = target
    }

    /**
     * Jumps directly to a specific 1-based page index.
     */
    fun goToPage(page: Int): Boolean {
        val target = if (pageCount > 0) page.coerceIn(1, pageCount) else page.coerceAtLeast(1)
        if (target != currentPage) {
            currentPage = target
            return true
        }
        return false
    }

    /**
     * Increases zoom by a fixed step up to MAX_ZOOM.
     */
    fun zoomIn(): Float {
        val target = zoom + ZOOM_STEP
        zoom = (kotlin.math.round(target * 100f) / 100f).coerceAtMost(MAX_ZOOM)
        return zoom
    }

    /**
     * Decreases zoom by a fixed step down to MIN_ZOOM.
     */
    fun zoomOut(): Float {
        val target = zoom - ZOOM_STEP
        zoom = (kotlin.math.round(target * 100f) / 100f).coerceAtLeast(MIN_ZOOM)
        return zoom
    }

    /**
     * Sets zoom to an explicit value within bounds.
     */
    fun setZoomLevel(level: Float) {
        zoom = level.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /**
     * Resets zoom to default 1.0f scale.
     */
    fun resetZoom() {
        zoom = 1f
    }

    /**
     * Rotates document page view 90 degrees clockwise.
     */
    fun rotateClockwise() {
        rotationAngle = (rotationAngle + 90f) % 360f
    }

    /**
     * Rotates document page view 90 degrees counter-clockwise.
     */
    fun rotateCounterClockwise() {
        rotationAngle = (rotationAngle - 90f + 360f) % 360f
    }

    /**
     * Resets zoom scale and rotation angle to defaults.
     */
    fun resetView() {
        zoom = 1f
        rotationAngle = 0f
    }

    /**
     * Toggles between FIT_PAGE and FIT_WIDTH scale modes.
     */
    fun toggleFitMode() {
        fitMode = if (fitMode == PdfFitMode.FIT_PAGE) PdfFitMode.FIT_WIDTH else PdfFitMode.FIT_PAGE
    }

    /**
     * Toggles visibility of page thumbnail grid drawer.
     */
    fun toggleGrid() {
        isGridVisible = !isGridVisible
    }

    companion object {
        const val MIN_ZOOM = 0.25f
        const val MAX_ZOOM = 5.0f
        const val ZOOM_STEP = 0.10f

        val Saver: Saver<PdfViewerState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.currentPage,
                    state.zoom,
                    state.rotationAngle,
                    state.fitMode.name,
                    state.isGridVisible
                )
            },
            restore = { list ->
                PdfViewerState(
                    initialPage = list[0] as Int,
                    initialZoom = list[1] as Float,
                    initialRotation = list[2] as Float,
                    initialFitMode = runCatching { PdfFitMode.valueOf(list[3] as String) }.getOrDefault(PdfFitMode.FIT_PAGE)
                ).apply {
                    if (list[4] as Boolean) {
                        toggleGrid()
                    }
                }
            }
        )
    }
}

/**
 * Creates and remembers a [PdfViewerState] instance across recompositions and configuration changes.
 */
@Composable
fun rememberPdfViewerState(
    initialPage: Int = 1,
    initialZoom: Float = 1f,
    initialRotation: Float = 0f,
    initialFitMode: PdfFitMode = PdfFitMode.FIT_PAGE
): PdfViewerState {
    return rememberSaveable(
        saver = PdfViewerState.Saver
    ) {
        PdfViewerState(
            initialPage = initialPage,
            initialZoom = initialZoom,
            initialRotation = initialRotation,
            initialFitMode = initialFitMode
        )
    }
}
