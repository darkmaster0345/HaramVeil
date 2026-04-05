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
import com.google.android.gms.tasks.Task
import com.haramveil.BuildConfig
import com.haramveil.utils.DispatcherProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MLKitOcrEngine(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) : OcrEngine {
    private val recognizerDelegate = lazy {
        createRecognizer()
    }
    private val recognizer get() = recognizerDelegate.value

    override suspend fun extractText(bitmap: Bitmap): OcrResult =
        withContext(dispatcherProvider.default) {
            if (!BuildConfig.INCLUDE_MLKIT) {
                return@withContext OcrResult("", 0f, 0L)
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

            if (!isAvailable()) {
                return@withContext OcrResult(
                    text = "",
                    confidence = 0f,
                    processingTimeMs = 0L,
                )
            }

            val startedAt = SystemClock.elapsedRealtime()
            val activeRecognizer = recognizer ?: return@withContext OcrResult("", 0f, 0L)
            val inputImage = createInputImage(bitmap) ?: return@withContext OcrResult("", 0f, 0L)
            val recognizedResult = process(activeRecognizer, inputImage)?.awaitResult()
                ?: return@withContext OcrResult("", 0f, 0L)
            val normalizedText = extractTextValue(recognizedResult)
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
        return BuildConfig.INCLUDE_MLKIT &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS &&
            isMlKitRuntimePresent()
    }

    fun close() {
        if (recognizerDelegate.isInitialized()) {
            runCatching {
                recognizerDelegate.value?.javaClass?.getMethod("close")?.invoke(recognizerDelegate.value)
            }
        }
    }

    private fun createRecognizer(): Any? =
        runCatching {
            val optionsClass = Class.forName(TextRecognizerOptionsClassName)
            val defaultOptions = optionsClass.getField(DefaultOptionsFieldName).get(null)
            val textRecognitionClass = Class.forName(TextRecognitionClassName)
            textRecognitionClass.methods
                .first { method -> method.name == GetClientMethodName && method.parameterTypes.size == 1 }
                .invoke(null, defaultOptions)
        }.getOrNull()

    private fun createInputImage(
        bitmap: Bitmap,
    ): Any? =
        runCatching {
            val inputImageClass = Class.forName(InputImageClassName)
            inputImageClass.getMethod(FromBitmapMethodName, Bitmap::class.java, Int::class.javaPrimitiveType!!)
                .invoke(null, bitmap, 0)
        }.getOrNull()

    private fun process(
        recognizer: Any,
        inputImage: Any,
    ): Task<*>? =
        runCatching {
            recognizer.javaClass.methods
                .first { method -> method.name == ProcessMethodName && method.parameterTypes.size == 1 }
                .invoke(recognizer, inputImage) as? Task<*>
        }.getOrNull()

    private fun extractTextValue(
        recognizedResult: Any,
    ): String =
        runCatching {
            recognizedResult.javaClass.getMethod(GetTextMethodName).invoke(recognizedResult) as? String
        }.getOrNull().orEmpty()

    private fun isMlKitRuntimePresent(): Boolean =
        runCatching {
            Class.forName(TextRecognitionClassName)
            Class.forName(TextRecognizerOptionsClassName)
            Class.forName(InputImageClassName)
        }.isSuccess

    private suspend fun Task<*>.awaitResult(): Any? =
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

    private companion object {
        const val TextRecognitionClassName = "com.google.mlkit.vision.text.TextRecognition"
        const val TextRecognizerOptionsClassName = "com.google.mlkit.vision.text.latin.TextRecognizerOptions"
        const val InputImageClassName = "com.google.mlkit.vision.common.InputImage"
        const val DefaultOptionsFieldName = "DEFAULT_OPTIONS"
        const val GetClientMethodName = "getClient"
        const val FromBitmapMethodName = "fromBitmap"
        const val ProcessMethodName = "process"
        const val GetTextMethodName = "getText"
    }
}
