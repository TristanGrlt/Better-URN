package org.better.urn.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
enum class ViewableFileType {
    IMAGE,
    PDF,
    AUDIO,
    VIDEO,
    DOCUMENT,
    OTHER
}

@Immutable
@Serializable
data class ViewableFile(
    val id: String,
    val title: String,
    val url: String,
    val mimeType: String? = null,
    val formattedFileSize: String? = null,
    val fileType: ViewableFileType = FileTypeUtils.detectType(title, mimeType)
) {
    /**
     * Determines whether this file can be previewed directly inside the application UI.
     */
    val isViewableInApp: Boolean
        get() = fileType == ViewableFileType.IMAGE ||
                fileType == ViewableFileType.PDF ||
                fileType == ViewableFileType.VIDEO
}
