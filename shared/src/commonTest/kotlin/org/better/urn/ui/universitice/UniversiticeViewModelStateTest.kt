package org.better.urn.ui.universitice

import kotlinx.coroutines.Dispatchers
import org.better.urn.data.ViewableFile
import org.better.urn.data.ViewableFileType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UniversiticeViewModelStateTest {

    @Test
    fun testViewModelInheritanceAndStateRetention() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        
        // Initial state
        assertNull(viewModel.uiState.value.selectedCourse)
        assertEquals("", viewModel.uiState.value.searchQuery)

        // Simulate search query change
        viewModel.onSearchQueryChange("Mathématiques")
        assertEquals("Mathématiques", viewModel.uiState.value.searchQuery)

        // Simulate opening file viewer
        val file = ViewableFile(
            id = "pdf_1",
            title = "Cours PDF",
            url = "https://example.com/doc.pdf",
            fileType = ViewableFileType.PDF
        )
        viewModel.openFileViewer(file)
        assertNotNull(viewModel.uiState.value.activeFileViewer)
        assertEquals("pdf_1", viewModel.uiState.value.activeFileViewer?.id)

        // Closing file viewer preserves search query
        viewModel.closeFileViewer()
        assertNull(viewModel.uiState.value.activeFileViewer)
        assertEquals("Mathématiques", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun testCourseSelectionAndClose() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)

        // Attempting to open a non-existent course from empty list does not change selected course
        viewModel.openCourse(101)
        assertNull(viewModel.uiState.value.selectedCourse)

        // Closing course resets selection and section states cleanly
        viewModel.closeCourse()
        assertNull(viewModel.uiState.value.selectedCourse)
        assertNull(viewModel.uiState.value.activeFileViewer)
    }
}
