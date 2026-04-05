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

package com.haramveil.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Build
import android.view.Display
import androidx.core.content.ContextCompat
import com.haramveil.data.models.ProtectionSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

class ScreenCaptureManager(
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) {

    suspend fun captureHaramClip(
        accessibilityService: AccessibilityService,
        settings: ProtectionSettings,
    ): Bitmap? = withContext(dispatcherProvider.default) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext null
        }

        val screenshotResult = awaitScreenshot(accessibilityService) ?: return@withContext null
        buildHaramClipBitmap(
            screenshotResult = screenshotResult,
            topCapturePercent = settings.topCapturePercent,
            middleCapturePercent = settings.middleCapturePercent,
        )
    }

    fun prepareForVisualModel(
        bitmap: Bitmap,
        inputSize: Int,
    ): Bitmap {
        val maxLongEdge = max(inputSize, inputSize * 2)
        val currentLongEdge = max(bitmap.width, bitmap.height)
        if (currentLongEdge <= maxLongEdge) {
            return bitmap
        }

        val scale = maxLongEdge.toFloat() / currentLongEdge.toFloat()
        val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaledBitmap.config == Bitmap.Config.RGB_565) {
            return scaledBitmap
        }

        return scaledBitmap.copy(Bitmap.Config.RGB_565, false).also { convertedBitmap ->
            if (convertedBitmap !== scaledBitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun awaitScreenshot(
        accessibilityService: AccessibilityService,
    ): AccessibilityService.ScreenshotResult? =
        suspendCancellableCoroutine { continuation ->
            accessibilityService.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                ContextCompat.getMainExecutor(accessibilityService),
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        if (continuation.isActive) {
                            continuation.resume(screenshot)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                },
            )
        }

    private fun buildHaramClipBitmap(
        screenshotResult: AccessibilityService.ScreenshotResult,
        topCapturePercent: Int,
        middleCapturePercent: Int,
    ): Bitmap? {
        val hardwareBuffer: HardwareBuffer = screenshotResult.hardwareBuffer ?: return null
        return try {
            val sourceBitmap = Bitmap.wrapHardwareBuffer(
                hardwareBuffer,
                screenshotResult.colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB),
            ) ?: return null

            try {
                combineCaptureZones(
                    sourceBitmap = sourceBitmap,
                    topCapturePercent = topCapturePercent,
                    middleCapturePercent = middleCapturePercent,
                )
            } finally {
                runCatching { sourceBitmap.recycle() }
            }
        } finally {
            hardwareBuffer.close()
        }
    }

    private fun combineCaptureZones(
        sourceBitmap: Bitmap,
        topCapturePercent: Int,
        middleCapturePercent: Int,
    ): Bitmap? {
        if (sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            return null
        }

        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val clampedTopPercent = topCapturePercent.coerceIn(minimumValue = 10, maximumValue = 45)
        val clampedMiddlePercent = middleCapturePercent.coerceIn(minimumValue = 20, maximumValue = 50)
        val topHeight = (height * (clampedTopPercent / 100f)).roundToInt().coerceAtLeast(1)
        val middleStart = (height * 0.35f).roundToInt().coerceIn(minimumValue = 0, maximumValue = height - 1)
        val middleHeight = (height * (clampedMiddlePercent / 100f)).roundToInt().coerceAtLeast(1)
        val middleEnd = (middleStart + middleHeight).coerceIn(minimumValue = middleStart + 1, maximumValue = height)
        val combinedHeight = topHeight + (middleEnd - middleStart)

        if (combinedHeight <= 0) {
            return null
        }

        return Bitmap.createBitmap(width, combinedHeight, Bitmap.Config.RGB_565).also { combinedBitmap ->
            val canvas = Canvas(combinedBitmap)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(
                sourceBitmap,
                Rect(0, 0, width, topHeight),
                Rect(0, 0, width, topHeight),
                null,
            )
            canvas.drawBitmap(
                sourceBitmap,
                Rect(0, middleStart, width, middleEnd),
                Rect(0, topHeight, width, combinedHeight),
                null,
            )
        }
    }
}
