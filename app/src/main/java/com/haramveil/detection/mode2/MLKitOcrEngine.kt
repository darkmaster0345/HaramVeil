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

package com.haramveil.detection.mode2

import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import android.os.SystemClock
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.haramveil.utils.DispatcherProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MLKitOcrEngine(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) : OcrEngine {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun extractText(bitmap: Bitmap): OcrResult =
        withContext(dispatcherProvider.default) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

            if (!isAvailable()) {
                return@withContext OcrResult(
                    text = "",
                    confidence = 0f,
                    processingTimeMs = 0L,
                )
            }

            val startedAt = SystemClock.elapsedRealtime()
            val recognizedText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult()
            val normalizedText = recognizedText.text
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .joinToString(separator = "\n")

            OcrResult(
                text = normalizedText,
                confidence = if (normalizedText.isBlank()) 0f else 0.85f,
                processingTimeMs = SystemClock.elapsedRealtime() - startedAt,
            )
        }

    override fun isAvailable(): Boolean {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    private suspend fun com.google.android.gms.tasks.Task<Text>.awaitResult(): Text =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
}
