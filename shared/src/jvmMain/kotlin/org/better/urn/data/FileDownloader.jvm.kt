package org.better.urn.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream

class JvmFileDownloader : FileDownloader {

    private val httpClient by lazy {
        HttpClient(CIO)
    }

    private val activeJobs = mutableMapOf<String, Boolean>()

    override fun downloadFile(file: ViewableFile): Flow<DownloadState> = flow {
        val downloadId = file.id
        activeJobs[downloadId] = true

        val initial = DownloadState(
            id = downloadId,
            file = file,
            status = DownloadStatus.DOWNLOADING,
            progress = 0f,
        )
        emit(initial)

        try {
            val response = httpClient.get(file.url) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            }

            if (!response.status.isSuccess()) {
                val errorMsg = "HTTP error ${response.status.value}"
                emit(
                    initial.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = errorMsg
                    )
                )
                return@flow
            }

            val totalBytes = response.contentLength() ?: -1L
            val channel: ByteReadChannel = response.bodyAsChannel()

            val sanitizedTitle = sanitizeFilename(file.title)
            val extension = FileTypeUtils.extractExtension(file.title).ifBlank {
                FileTypeUtils.extractExtension(file.url)
            }
            val fileName = if (sanitizedTitle.contains('.')) sanitizedTitle else "$sanitizedTitle.$extension"

            val userHome = System.getProperty("user.home") ?: "."
            val downloadsDir = File(userHome, "Downloads/BetterURN")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val outputFile = File(downloadsDir, fileName)
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var downloadedBytes = 0L
            var lastEmittedProgress = 0f

            while (!channel.isClosedForRead && (activeJobs[downloadId] == true)) {
                val read = channel.readAvailable(buffer, 0, buffer.size)
                if (read <= 0) break

                outputStream.write(buffer, 0, read)
                downloadedBytes += read

                val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                if (progress - lastEmittedProgress >= 0.02f || progress >= 1f) {
                    lastEmittedProgress = progress
                    emit(
                        initial.copy(
                            status = DownloadStatus.DOWNLOADING,
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    )
                }
            }

            withContext(Dispatchers.IO) {
                outputStream.flush()
                outputStream.close()
            }

            if (activeJobs[downloadId] != true) {
                if (outputFile.exists()) outputFile.delete()
                emit(initial.copy(status = DownloadStatus.FAILED, errorMessage = "Téléchargement annulé"))
                return@flow
            }

            val completedState = initial.copy(
                status = DownloadStatus.COMPLETED,
                progress = 1f,
                downloadedBytes = downloadedBytes,
                totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                filePath = outputFile.absolutePath
            )
            emit(completedState)

        } catch (e: Exception) {
            emit(initial.copy(status = DownloadStatus.FAILED, errorMessage = e.message ?: "Échec du téléchargement"))
        } finally {
            activeJobs.remove(downloadId)
        }
    }.flowOn(Dispatchers.IO)

    override fun openFile(filePath: String, mimeType: String?): Boolean {
        val targetFile = File(filePath)
        if (!targetFile.exists()) return false

        return try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(targetFile)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun cancelDownload(downloadId: String) {
        activeJobs[downloadId] = false
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
}

actual fun createPlatformFileDownloader(): FileDownloader = JvmFileDownloader()
