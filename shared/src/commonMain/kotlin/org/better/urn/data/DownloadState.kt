package org.better.urn.data

import androidx.compose.runtime.Immutable

@Immutable
enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

@Immutable
data class DownloadState(
    val id: String,
    val file: ViewableFile,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val filePath: String? = null,
    val errorMessage: String? = null
)
