/*
 * HaramVeil
 * Copyright (C) 2026 HaramVeil Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.haramveil.data.models

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.haramveil.MainActivity
import com.haramveil.R
import com.haramveil.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface ModelDownloadState {
    data class Available(
        val file: File,
    ) : ModelDownloadState

    data class Downloaded(
        val file: File,
    ) : ModelDownloadState

    data class Failed(
        val reason: String,
    ) : ModelDownloadState
}

class ModelDownloadManager(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) {

    suspend fun downloadRapidOcrModelIfNeeded(): ModelDownloadState =
        withContext(dispatcherProvider.io) {
            if (isModelReady()) {
                return@withContext ModelDownloadState.Available(modelFile())
            }

            val modelsDirectory = ensureModelsDirectory()
            val targetFile = File(modelsDirectory, ModelFileName)
            val temporaryFile = File(modelsDirectory, "$ModelFileName.part")
            createNotificationChannel()
            postProgressNotification(progressPercent = null, message = "Preparing FOSS OCR model download")

            val connection = (URL(ModelUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }

            try {
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    temporaryFile.delete()
                    cancelProgressNotification()
                    return@withContext ModelDownloadState.Failed(
                        reason = "Model download failed with HTTP ${connection.responseCode}.",
                    )
                }

                val contentLength = connection.contentLengthLong
                val digest = MessageDigest.getInstance("SHA-256")
                var bytesCopied = 0L
                var lastNotifiedProgress = -1

                connection.inputStream.use { inputStream ->
                    FileOutputStream(temporaryFile).use { outputStream ->
                        val buffer = ByteArray(DownloadBufferSizeBytes)
                        while (true) {
                            val readCount = inputStream.read(buffer)
                            if (readCount <= 0) {
                                break
                            }

                            outputStream.write(buffer, 0, readCount)
                            digest.update(buffer, 0, readCount)
                            bytesCopied += readCount

                            if (contentLength > 0) {
                                val progressPercent = ((bytesCopied * 100) / contentLength).toInt()
                                if (progressPercent != lastNotifiedProgress) {
                                    lastNotifiedProgress = progressPercent
                                    postProgressNotification(
                                        progressPercent = progressPercent,
                                        message = "Downloading private OCR model",
                                    )
                                }
                            }
                        }
                    }
                }

                val actualChecksum = digest.digest().toHexString()
                if (!actualChecksum.equals(ModelSha256, ignoreCase = true)) {
                    temporaryFile.delete()
                    cancelProgressNotification()
                    postSetupReminderNotification()
                    return@withContext ModelDownloadState.Failed(
                        reason = "RapidOCR checksum verification failed.",
                    )
                }

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                temporaryFile.copyTo(targetFile, overwrite = true)
                temporaryFile.delete()

                cancelProgressNotification()
                postSetupReminderNotification(
                    title = "FOSS OCR ready",
                    message = "The private OCR model has been stored on-device. HaramVeil will not use the network for this model again.",
                )

                ModelDownloadState.Downloaded(targetFile)
            } catch (exception: Exception) {
                temporaryFile.delete()
                cancelProgressNotification()
                ModelDownloadState.Failed(
                    reason = exception.message ?: "RapidOCR model download failed.",
                )
            } finally {
                connection.disconnect()
            }
        }

    fun isModelReady(): Boolean {
        val file = modelFile()
        return file.isFile && verifyChecksum(file)
    }

    @SuppressLint("MissingPermission")
    fun postSetupReminderNotification(
        title: String = "Finish FOSS OCR setup",
        message: String = "Download the private OCR model to enable the FOSS text engine.",
    ) {
        createNotificationChannel()
        NotificationManagerCompat.from(context).notify(
            SetupReminderNotificationId,
            NotificationCompat.Builder(context, NotificationChannelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(buildMainActivityPendingIntent())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build(),
        )
    }

    fun modelFile(): File = File(ensureModelsDirectory(), ModelFileName)

    private fun ensureModelsDirectory(): File =
        File(context.filesDir, ModelsDirectoryName).apply { mkdirs() }

    private fun verifyChecksum(file: File): Boolean {
        if (!file.isFile) {
            return false
        }

        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(DownloadBufferSizeBytes)
                while (true) {
                    val readCount = inputStream.read(buffer)
                    if (readCount <= 0) {
                        break
                    }
                    digest.update(buffer, 0, readCount)
                }
            }
            digest.digest().toHexString().equals(ModelSha256, ignoreCase = true)
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    private fun postProgressNotification(
        progressPercent: Int?,
        message: String,
    ) {
        val builder = NotificationCompat.Builder(context, NotificationChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("HaramVeil model download")
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(buildMainActivityPendingIntent())

        if (progressPercent != null) {
            builder.setProgress(100, progressPercent, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        NotificationManagerCompat.from(context).notify(DownloadNotificationId, builder.build())
    }

    private fun cancelProgressNotification() {
        NotificationManagerCompat.from(context).cancel(DownloadNotificationId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val existingChannel = notificationManager.getNotificationChannel(NotificationChannelId)
        if (existingChannel != null) {
            return
        }

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelId,
                "HaramVeil Model Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "One-time private model setup progress for HaramVeil."
            },
        )
    }

    private fun buildMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            5005,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }

    companion object {
        private const val NotificationChannelId = "haramveil_model_downloads"
        private const val DownloadNotificationId = 5105
        private const val SetupReminderNotificationId = 5106
        private const val ModelsDirectoryName = "models"
        private const val ModelFileName = "rapidocr.onnx"
        private const val DownloadBufferSizeBytes = 8_192

        const val ModelUrl =
            "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.7.0/onnx/PP-OCRv5/rec/latin_PP-OCRv5_rec_mobile_infer.onnx"
        const val ModelSha256 =
            "b20bd37c168a570f583afbc8cd7925603890efbcdc000a59e22c269d160b5f5a"
    }
}
