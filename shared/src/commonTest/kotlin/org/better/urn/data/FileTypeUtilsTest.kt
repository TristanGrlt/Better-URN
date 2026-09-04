package org.better.urn.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FileTypeUtilsTest {

    @Test
    fun testDetectTypeFromMimeType() {
        assertEquals(ViewableFileType.IMAGE, FileTypeUtils.detectType("file.unknown", "image/png"))
        assertEquals(ViewableFileType.IMAGE, FileTypeUtils.detectType("file.unknown", "IMAGE/JPEG"))
        assertEquals(ViewableFileType.PDF, FileTypeUtils.detectType("file.unknown", "application/pdf"))
        assertEquals(ViewableFileType.AUDIO, FileTypeUtils.detectType("file.unknown", "audio/mpeg"))
        assertEquals(ViewableFileType.VIDEO, FileTypeUtils.detectType("file.unknown", "video/mp4"))
        assertEquals(ViewableFileType.DOCUMENT, FileTypeUtils.detectType("file.unknown", "text/plain"))
    }

    @Test
    fun testDetectTypeFromExtension() {
        assertEquals(ViewableFileType.IMAGE, FileTypeUtils.detectType("schema.png"))
        assertEquals(ViewableFileType.IMAGE, FileTypeUtils.detectType("photo.JPG?token=xyz"))
        assertEquals(ViewableFileType.IMAGE, FileTypeUtils.detectType("https://example.com/vector.svg#fragment"))
        assertEquals(ViewableFileType.PDF, FileTypeUtils.detectType("cours.pdf"))
        assertEquals(ViewableFileType.AUDIO, FileTypeUtils.detectType("podcast.mp3"))
        assertEquals(ViewableFileType.VIDEO, FileTypeUtils.detectType("lecture.mp4"))
        assertEquals(ViewableFileType.DOCUMENT, FileTypeUtils.detectType("notes.docx"))
        assertEquals(ViewableFileType.OTHER, FileTypeUtils.detectType("archive.zip"))
    }

    @Test
    fun testExtractExtension() {
        assertEquals("png", FileTypeUtils.extractExtension("image.PNG"))
        assertEquals("pdf", FileTypeUtils.extractExtension("http://host/path/file.pdf?wstoken=123"))
        assertEquals("gz", FileTypeUtils.extractExtension("data.tar.gz"))
        assertEquals("", FileTypeUtils.extractExtension("file_without_extension"))
    }

    @Test
    fun testCourseModuleToViewableFile() {
        val module = CourseModule(
            id = 101,
            name = "Document d'exemple.png",
            modname = "resource",
            url = "https://moodle.univ.fr/mod/resource/view.php?id=101",
            contents = listOf(
                ModuleContent(
                    filename = "diagramme.png",
                    fileurl = "https://moodle.univ.fr/webservice/pluginfile.php/101/mod_resource/content/1/diagramme.png",
                    mimetype = "image/png",
                    filesize = 204800
                )
            )
        )

        val viewable = module.toViewableFile("mytoken123")
        assertNotNull(viewable)
        assertEquals("101", viewable.id)
        assertEquals("Document d'exemple.png", viewable.title)
        assertEquals(ViewableFileType.IMAGE, viewable.fileType)
        assertEquals("200.0 KB", viewable.formattedFileSize)
        assertEquals("https://moodle.univ.fr/webservice/pluginfile.php/101/mod_resource/content/1/diagramme.png?token=mytoken123", viewable.url)
    }

    @Test
    fun testCourseModuleToViewableFileNullWhenNoUrl() {
        val module = CourseModule(
            id = 202,
            name = "Module vide",
            modname = "label"
        )
        assertNull(module.toViewableFile("token"))
    }
}
