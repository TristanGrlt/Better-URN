package org.better.urn.ui.universitice

import kotlinx.coroutines.Dispatchers
import org.better.urn.data.CourseModule
import org.better.urn.data.ModuleContent
import org.better.urn.data.ViewableFile
import org.better.urn.data.ViewableFileType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileViewerViewModelTest {

    @Test
    fun testOpenFileViewerAndClose() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val file = ViewableFile(
            id = "1",
            title = "Test Image",
            url = "https://example.com/test.png",
            fileType = ViewableFileType.IMAGE
        )

        assertNull(viewModel.uiState.value.activeFileViewer)

        viewModel.openFileViewer(file)
        val active = viewModel.uiState.value.activeFileViewer
        assertNotNull(active)
        assertEquals("1", active.id)
        assertEquals("Test Image", active.title)

        viewModel.closeFileViewer()
        assertNull(viewModel.uiState.value.activeFileViewer)
    }

    @Test
    fun testOpenModuleFileWithValidImage() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val module = CourseModule(
            id = 42,
            name = "Photo du cours.jpg",
            modname = "resource",
            url = "https://moodle.univ.fr/mod/resource/view.php?id=42",
            contents = listOf(
                ModuleContent(
                    filename = "photo.jpg",
                    fileurl = "https://moodle.univ.fr/webservice/pluginfile.php/42/mod_resource/content/1/photo.jpg",
                    mimetype = "image/jpeg"
                )
            )
        )

        val opened = viewModel.openModuleFile(module)
        assertTrue(opened)

        val active = viewModel.uiState.value.activeFileViewer
        assertNotNull(active)
        assertEquals("42", active.id)
        assertEquals("Photo du cours.jpg", active.title)
        assertEquals(ViewableFileType.IMAGE, active.fileType)
    }

    @Test
    fun testOpenModuleFileWithValidVideo() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val module = CourseModule(
            id = 84,
            name = "Tutoriel Chapitre 1.mp4",
            modname = "resource",
            url = "https://moodle.univ.fr/mod/resource/view.php?id=84",
            contents = listOf(
                ModuleContent(
                    filename = "tuto.mp4",
                    fileurl = "https://moodle.univ.fr/webservice/pluginfile.php/84/mod_resource/content/1/tuto.mp4",
                    mimetype = "video/mp4",
                    filesize = 15728640
                )
            )
        )

        val opened = viewModel.openModuleFile(module)
        assertTrue(opened)

        val active = viewModel.uiState.value.activeFileViewer
        assertNotNull(active)
        assertEquals("84", active.id)
        assertEquals("Tutoriel Chapitre 1.mp4", active.title)
        assertEquals(ViewableFileType.VIDEO, active.fileType)
        assertEquals("15.0 MB", active.formattedFileSize)
    }

    @Test
    fun testOpenModuleFileWithValidPdf() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val module = CourseModule(
            id = 128,
            name = "Support de cours.pdf",
            modname = "resource",
            url = "https://moodle.univ.fr/mod/resource/view.php?id=128",
            contents = listOf(
                ModuleContent(
                    filename = "cours.pdf",
                    fileurl = "https://moodle.univ.fr/webservice/pluginfile.php/128/mod_resource/content/1/cours.pdf",
                    mimetype = "application/pdf",
                    filesize = 2097152
                )
            )
        )

        val opened = viewModel.openModuleFile(module)
        assertTrue(opened)

        val active = viewModel.uiState.value.activeFileViewer
        assertNotNull(active)
        assertEquals("128", active.id)
        assertEquals("Support de cours.pdf", active.title)
        assertEquals(ViewableFileType.PDF, active.fileType)
        assertEquals("2.0 MB", active.formattedFileSize)
    }

    @Test
    fun testOpenModuleFileWithoutUrlFails() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val module = CourseModule(
            id = 99,
            name = "Etiquette sans lien",
            modname = "label"
        )

        val opened = viewModel.openModuleFile(module)
        assertFalse(opened)
        assertNull(viewModel.uiState.value.activeFileViewer)
    }

    @Test
    fun testCloseCourseClearsActiveFileViewer() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val file = ViewableFile(
            id = "5",
            title = "Document",
            url = "https://example.com/doc.pdf",
            fileType = ViewableFileType.PDF
        )

        viewModel.openFileViewer(file)
        assertNotNull(viewModel.uiState.value.activeFileViewer)

        viewModel.closeCourse()
        assertNull(viewModel.uiState.value.activeFileViewer)
    }
}
