package org.better.urn.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileOutputStream

private const val CHANNEL_ID = "better_urn_file_downloads"
private const val CHANNEL_NAME = "Téléchargements"

class AndroidFileDownloader(
    private val getContext: () -> Context? = { AndroidContextProvider.context },
) : FileDownloader {

    private val httpClient by lazy {
        HttpClient(CIO)
    }

    private val activeJobs = mutableMapOf<String, Boolean>()

    private fun ensureNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications de progression de téléchargement de fichiers"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun downloadFile(file: ViewableFile): Flow<DownloadState> = flow {
        val downloadId = file.id
        activeJobs[downloadId] = true

        val initial = DownloadState(
            id = downloadId,
            file = file,
            status = DownloadStatus.DOWNLOADING,
            progress = 0f
        )
        emit(initial)

        val context = getContext()
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.let {
            ensureNotificationChannel(it)
        }

        val notificationId = downloadId.hashCode()

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
                updateErrorNotification(context, notificationManager, notificationId, file, errorMsg)
                return@flow
            }

            val totalBytes = response.contentLength() ?: -1L
            val channel: ByteReadChannel = response.bodyAsChannel()

            val sanitizedTitle = sanitizeFilename(file.title)
            val extension = FileTypeUtils.extractExtension(file.title).ifBlank {
                FileTypeUtils.extractExtension(file.url)
            }
            val fileName = if (sanitizedTitle.contains('.')) sanitizedTitle else "$sanitizedTitle.$extension"

            val downloadsDir = context?.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: context?.filesDir
                ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")

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
                    val current = initial.copy(
                        status = DownloadStatus.DOWNLOADING,
                        progress = progress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes
                    )
                    emit(current)
                    updateProgressNotification(context, notificationManager, notificationId, file, progress)
                }
            }

            withContext(Dispatchers.IO) {
                outputStream.flush()
                outputStream.close()
            }

            if (activeJobs[downloadId] != true) {
                if (outputFile.exists()) outputFile.delete()
                emit(initial.copy(status = DownloadStatus.FAILED, errorMessage = "Téléchargement annulé"))
                notificationManager?.cancel(notificationId)
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

            updateCompleteNotification(context, notificationManager, notificationId, file, outputFile)

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Échec du téléchargement"
            emit(initial.copy(status = DownloadStatus.FAILED, errorMessage = errorMsg))
            updateErrorNotification(context, notificationManager, notificationId, file, errorMsg)
        } finally {
            activeJobs.remove(downloadId)
        }
    }.flowOn(Dispatchers.IO)

    override fun openFile(filePath: String, mimeType: String?): Boolean {
        val context = getContext() ?: return false
        val targetFile = File(filePath)
        if (!targetFile.exists()) return false

        val authority = "${context.packageName}.fileprovider"
        val contentUri = try {
            FileProvider.getUriForFile(context, authority, targetFile)
        } catch (_: Exception) {
            return false
        }

        val resolvedMime = mimeType?.ifBlank { null }
            ?: getMimeTypeFromFile(targetFile)
            ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, resolvedMime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun cancelDownload(downloadId: String) {
        activeJobs[downloadId] = false
    }

    private fun updateProgressNotification(
        context: Context?,
        notificationManager: NotificationManager?,
        notificationId: Int,
        file: ViewableFile,
        progress: Float
    ) {
        if (context == null || notificationManager == null) return
        val percent = (progress * 100).toInt()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }

        builder.apply {
            setContentTitle(file.title)
            setContentText("Téléchargement en cours... $percent%")
            setSmallIcon(android.R.drawable.stat_sys_download)
            setProgress(100, percent, progress == 0f)
            setOngoing(true)
            setAutoCancel(false)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun updateCompleteNotification(
        context: Context?,
        notificationManager: NotificationManager?,
        notificationId: Int,
        file: ViewableFile,
        outputFile: File
    ) {
        if (context == null || notificationManager == null) return

        val authority = "${context.packageName}.fileprovider"
        val contentUri = runCatching {
            FileProvider.getUriForFile(context, authority, outputFile)
        }.getOrNull()

        val openIntent = if (contentUri != null) {
            val resolvedMime = file.mimeType ?: getMimeTypeFromFile(outputFile) ?: "*/*"
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else null

        val pendingIntent = if (openIntent != null) {
            PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }

        builder.apply {
            setContentTitle(file.title)
            setContentText("Téléchargement terminé. Appuyez pour ouvrir.")
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)
            setOngoing(false)
            setAutoCancel(true)
            if (pendingIntent != null) {
                setContentIntent(pendingIntent)
            }
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun updateErrorNotification(
        context: Context?,
        notificationManager: NotificationManager?,
        notificationId: Int,
        file: ViewableFile,
        errorMessage: String
    ) {
        if (context == null || notificationManager == null) return

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }

        builder.apply {
            setContentTitle(file.title)
            setContentText("Échec du téléchargement : $errorMessage")
            setSmallIcon(android.R.drawable.stat_notify_error)
            setProgress(0, 0, false)
            setOngoing(false)
            setAutoCancel(true)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    private fun getMimeTypeFromFile(file: File): String? {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}

actual fun createPlatformFileDownloader(): FileDownloader = AndroidFileDownloader()
