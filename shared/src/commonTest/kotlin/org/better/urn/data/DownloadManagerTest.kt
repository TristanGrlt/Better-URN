package org.better.urn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.better.urn.ui.universitice.UniversiticeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeFileDownloader : FileDownloader {
    val downloadedFiles = mutableListOf<ViewableFile>()
    val openedFilePaths = mutableListOf<String>()
    val cancelledIds = mutableListOf<String>()

    override fun downloadFile(file: ViewableFile): Flow<DownloadState> = flow {
        downloadedFiles.add(file)
        emit(DownloadState(id = file.id, file = file, status = DownloadStatus.DOWNLOADING, progress = 0.5f))
        emit(DownloadState(id = file.id, file = file, status = DownloadStatus.COMPLETED, progress = 1.0f, filePath = "/downloads/${file.title}"))
    }

    override fun openFile(filePath: String, mimeType: String?): Boolean {
        openedFilePaths.add(filePath)
        return true
    }

    override fun cancelDownload(downloadId: String) {
        cancelledIds.add(downloadId)
    }
}

class DownloadManagerTest {

    @Test
    fun testIsViewableInAppClassification() {
        val image = ViewableFile("1", "img.png", "http://x", fileType = ViewableFileType.IMAGE)
        val pdf = ViewableFile("2", "doc.pdf", "http://x", fileType = ViewableFileType.PDF)
        val video = ViewableFile("3", "mov.mp4", "http://x", fileType = ViewableFileType.VIDEO)
        val doc = ViewableFile("4", "notes.docx", "http://x", fileType = ViewableFileType.DOCUMENT)
        val audio = ViewableFile("5", "sound.mp3", "http://x", fileType = ViewableFileType.AUDIO)
        val zip = ViewableFile("6", "archive.zip", "http://x", fileType = ViewableFileType.OTHER)

        assertTrue(image.isViewableInApp)
        assertTrue(pdf.isViewableInApp)
        assertTrue(video.isViewableInApp)
        assertFalse(doc.isViewableInApp)
        assertFalse(audio.isViewableInApp)
        assertFalse(zip.isViewableInApp)
    }

    @Test
    fun testNonViewableModuleTriggersDirectDownload() {
        val fakeDownloader = FakeFileDownloader()
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined, fakeDownloader)

        val docModule = CourseModule(
            id = 555,
            name = "Exercices_Ch1.docx",
            modname = "resource",
            url = "https://moodle.univ.fr/mod/resource/view.php?id=555",
            contents = listOf(
                ModuleContent(
                    filename = "Exercices_Ch1.docx",
                    fileurl = "https://moodle.univ.fr/webservice/pluginfile.php/555/mod_resource/content/1/Exercices_Ch1.docx",
                    mimetype = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    filesize = 1048576,
                )
            )
        )

        val opened = viewModel.openModuleFile(docModule)
        assertTrue(opened)

        // Non-viewable files should NOT set activeFileViewer
        assertNull(viewModel.uiState.value.activeFileViewer)

        // Instead, it should trigger direct download via FileDownloader
        assertEquals(1, fakeDownloader.downloadedFiles.size)
        assertEquals("Exercices_Ch1.docx", fakeDownloader.downloadedFiles.first().title)

        val downloadState = viewModel.uiState.value.downloadState
        assertNotNull(downloadState)
        assertEquals(DownloadStatus.COMPLETED, downloadState.status)
        assertEquals("/downloads/Exercices_Ch1.docx", downloadState.filePath)
    }

    @Test
    fun testForumModuleIsNotDownloadable() {
        val fakeDownloader = FakeFileDownloader()
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined, fakeDownloader)

        val forumModule = CourseModule(
            id = 999,
            name = "Forum de discussion",
            modname = "forum",
            url = "https://moodle.univ.fr/mod/forum/view.php?id=999"
        )

        val opened = viewModel.openModuleFile(forumModule)
        assertFalse(opened)
        assertEquals(0, fakeDownloader.downloadedFiles.size)
    }

    @Test
    fun testOpenAndDismissDownloadedFileState() {
        val fakeDownloader = FakeFileDownloader()
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined, fakeDownloader)

        val file = ViewableFile("777", "Archive.zip", "https://moodle.univ.fr/archive.zip", fileType = ViewableFileType.OTHER)

        viewModel.downloadFile(file)
        assertNotNull(viewModel.uiState.value.downloadState)

        viewModel.openDownloadedFile()
        assertEquals(1, fakeDownloader.openedFilePaths.size)
        assertEquals("/downloads/Archive.zip", fakeDownloader.openedFilePaths.first())

        viewModel.dismissDownloadNotification()
        assertNull(viewModel.uiState.value.downloadState)
    }
}
