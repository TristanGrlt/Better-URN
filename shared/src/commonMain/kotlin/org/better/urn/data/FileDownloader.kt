package org.better.urn.data

import kotlinx.coroutines.flow.Flow

/**
 * Cross-platform interface for executing file downloads and launching downloaded files.
 */
interface FileDownloader {
    /**
     * Downloads the given [file] with real-time state and progress updates.
     */
    fun downloadFile(file: ViewableFile): Flow<DownloadState>

    /**
     * Attempts to open a previously downloaded file located at [filePath].
     * Returns true if successfully launched by platform file handler.
     */
    fun openFile(filePath: String, mimeType: String?): Boolean

    /**
     * Cancels an active download by id.
     */
    fun cancelDownload(downloadId: String)
}

/**
 * Factory function producing the platform-specific implementation of [FileDownloader].
 */
expect fun createPlatformFileDownloader(): FileDownloader
