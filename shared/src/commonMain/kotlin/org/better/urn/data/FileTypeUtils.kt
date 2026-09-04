package org.better.urn.data

object FileTypeUtils {
    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif"
    )
    private val PDF_EXTENSIONS = setOf("pdf")
    private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "avi", "mov")
    private val DOC_EXTENSIONS = setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "txt")

    /**
     * Detects the category of a file based on its MIME type or extension.
     */
    fun detectType(urlOrFilename: String, mimeType: String? = null): ViewableFileType {
        if (!mimeType.isNullOrBlank()) {
            val normalizedMime = mimeType.lowercase().trim()
            when {
                normalizedMime.startsWith("image/") -> return ViewableFileType.IMAGE
                normalizedMime == "application/pdf" -> return ViewableFileType.PDF
                normalizedMime.startsWith("audio/") -> return ViewableFileType.AUDIO
                normalizedMime.startsWith("video/") -> return ViewableFileType.VIDEO
                normalizedMime.contains("officedocument") ||
                        normalizedMime.contains("msword") ||
                        normalizedMime.contains("msexcel") ||
                        normalizedMime.contains("text/plain") -> return ViewableFileType.DOCUMENT
            }
        }

        val extension = extractExtension(urlOrFilename)
        return when (extension) {
            in IMAGE_EXTENSIONS -> ViewableFileType.IMAGE
            in PDF_EXTENSIONS -> ViewableFileType.PDF
            in AUDIO_EXTENSIONS -> ViewableFileType.AUDIO
            in VIDEO_EXTENSIONS -> ViewableFileType.VIDEO
            in DOC_EXTENSIONS -> ViewableFileType.DOCUMENT
            else -> ViewableFileType.OTHER
        }
    }

    /**
     * Extracts lowercase file extension from URL or filename, stripping query parameters.
     */
    fun extractExtension(urlOrFilename: String): String {
        val cleanPath = urlOrFilename.substringBefore('?').substringBefore('#')
        val lastSegment = cleanPath.substringAfterLast('/')
        val ext = lastSegment.substringAfterLast('.', "")
        return ext.lowercase()
    }
}

/**
     * Converts a CourseModule into a ViewableFile if it has a valid target URL.
     */
fun CourseModule.toViewableFile(token: String): ViewableFile? {
    val primaryUrl = getPrimaryUrl(token) ?: return null
    val firstContent = contents?.firstOrNull()
    val mime = firstContent?.mimetype
    val fileName = firstContent?.filename ?: name
    val sizeFormatted = firstContent?.getFormattedFileSize()
    val detectedType = FileTypeUtils.detectType(fileName, mime)

    return ViewableFile(
        id = id.toString(),
        title = name,
        url = primaryUrl,
        mimeType = mime,
        formattedFileSize = sizeFormatted,
        fileType = detectedType
    )
}
