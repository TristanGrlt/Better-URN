package org.better.urn.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfViewerStateTest {

    @Test
    fun testInitialStateAndClamping() {
        val state = PdfViewerState(
            initialPage = 0,
            initialZoom = 10f,
            initialRotation = -90f
        )
        assertEquals(1, state.currentPage)
        assertEquals(PdfViewerState.MAX_ZOOM, state.zoom)
        assertEquals(270f, state.rotationAngle)
        assertEquals(PdfFitMode.FIT_PAGE, state.fitMode)
    }

    @Test
    fun testPageNavigationAndBounds() {
        val state = PdfViewerState()
        state.updatePageCount(5)
        assertEquals(5, state.pageCount)
        assertEquals(1, state.currentPage)

        assertTrue(state.nextPage())
        assertEquals(2, state.currentPage)

        assertTrue(state.goToPage(4))
        assertEquals(4, state.currentPage)

        // Beyond maximum page count
        assertTrue(state.goToPage(10))
        assertEquals(5, state.currentPage)

        assertTrue(state.previousPage())
        assertEquals(4, state.currentPage)

        // Before first page
        state.goToPage(1)
        assertFalse(state.previousPage())
        assertEquals(1, state.currentPage)
    }

    @Test
    fun testZoomOperations() {
        val state = PdfViewerState()
        assertEquals(1.0f, state.zoom)

        state.zoomIn()
        assertEquals(1.25f, state.zoom)

        state.zoomOut()
        assertEquals(1.0f, state.zoom)

        state.setZoomLevel(0.2f)
        assertEquals(PdfViewerState.MIN_ZOOM, state.zoom)

        state.resetZoom()
        assertEquals(1.0f, state.zoom)
    }

    @Test
    fun testRotationOperations() {
        val state = PdfViewerState()
        assertEquals(0f, state.rotationAngle)

        state.rotateClockwise()
        assertEquals(90f, state.rotationAngle)

        state.rotateClockwise()
        assertEquals(180f, state.rotationAngle)

        state.rotateCounterClockwise()
        assertEquals(90f, state.rotationAngle)

        state.resetView()
        assertEquals(0f, state.rotationAngle)
        assertEquals(1.0f, state.zoom)
    }

    @Test
    fun testFitModeAndGridToggle() {
        val state = PdfViewerState()
        assertEquals(PdfFitMode.FIT_PAGE, state.fitMode)
        assertFalse(state.isGridVisible)

        state.toggleFitMode()
        assertEquals(PdfFitMode.FIT_WIDTH, state.fitMode)

        state.toggleGrid()
        assertTrue(state.isGridVisible)
    }
}
