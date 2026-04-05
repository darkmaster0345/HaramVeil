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

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import com.haramveil.data.local.ProtectionPreferencesRepository
import com.haramveil.data.local.StatsRepository
import com.haramveil.data.models.ProtectionSettings
import com.haramveil.utils.DetectionBus
import com.haramveil.utils.DetectionEvent
import com.haramveil.utils.DetectionTriggerMode
import com.haramveil.utils.DispatcherProvider
import com.haramveil.utils.Mode1WakeRequest
import com.haramveil.utils.ScreenCaptureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Mode2Processor(
    context: Context,
    private val accessibilityServiceProvider: () -> AccessibilityService?,
    private val protectionPreferencesRepository: ProtectionPreferencesRepository,
    private val settingsProvider: () -> ProtectionSettings,
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) {
    private val processorScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)
    private val screenCaptureManager = ScreenCaptureManager(dispatcherProvider)
    private val statsRepository = StatsRepository.getInstance(context)
    private val ocrEngineSelector = OcrEngineSelector(
        context = context,
        protectionPreferencesRepository = protectionPreferencesRepository,
        dispatcherProvider = dispatcherProvider,
    )
    private var processorJob: Job? = null

    fun start() {
        if (processorJob != null) {
            return
        }

        processorJob = processorScope.launch(dispatcherProvider.default) {
            DetectionBus.events
                .filterIsInstance<DetectionEvent.Mode1Triggered>()
                .collectLatest { event ->
                    processTrigger(event.trigger)
                }
        }
    }

    fun stop() {
        processorJob?.cancel()
        processorJob = null
    }

    fun destroy() {
        stop()
        processorScope.cancel()
    }

    private suspend fun processTrigger(trigger: Mode1WakeRequest) {
        withContext(dispatcherProvider.default) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

            val settings = settingsProvider()
            if (!settings.mode2Enabled || !trigger.wakeMode2) {
                return@withContext
            }

            val accessibilityService = accessibilityServiceProvider() ?: run {
                DetectionBus.publishMode2Clear(
                    packageName = trigger.scanResult.packageName,
                    details = "Accessibility service instance was unavailable for OCR capture.",
                )
                return@withContext
            }

            val captureBitmap = screenCaptureManager.captureHaramClip(accessibilityService, settings) ?: run {
                DetectionBus.publishMode2Clear(
                    packageName = trigger.scanResult.packageName,
                    details = "Screenshot capture is unavailable on this device or Android version.",
                )
                return@withContext
            }

            try {
                val ocrEngine = ocrEngineSelector.select(settings.selectedTextEngine)
                val ocrResult = ocrEngine.extractText(captureBitmap)
                val matchedKeyword = findMatchedKeyword(
                    extractedText = ocrResult.text,
                    keywordBlocklist = settings.keywordBlocklist,
                )

                if (matchedKeyword != null) {
                    val matchDetails =
                        "OCR matched keyword \"$matchedKeyword\" using ${ocrEngine.label()} in ${ocrResult.processingTimeMs}ms."
                    statsRepository.logBlock(
                        packageName = trigger.scanResult.packageName,
                        mode = 2,
                        detail = matchedKeyword,
                        lockdownMs = settings.lockdownDurationMs,
                    )
                    DetectionBus.publishMode2Triggered(
                        packageName = trigger.scanResult.packageName,
                        matchDetails = matchDetails,
                    )
                    DetectionBus.publishVeilRequested(
                        packageName = trigger.scanResult.packageName,
                        triggerMode = DetectionTriggerMode.MODE_2,
                        matchDetails = matchDetails,
                    )
                } else {
                    DetectionBus.publishMode2Clear(
                        packageName = trigger.scanResult.packageName,
                        details = if (ocrResult.text.isBlank()) {
                            "OCR completed without readable text."
                        } else {
                            "OCR found no blocked keywords."
                        },
                    )
                }
            } finally {
                recycleBitmap(captureBitmap)
            }
        }
    }

    private fun findMatchedKeyword(
        extractedText: String,
        keywordBlocklist: List<String>,
    ): String? {
        if (extractedText.isBlank() || keywordBlocklist.isEmpty()) {
            return null
        }

        return keywordBlocklist.firstOrNull { entry ->
            val trimmedEntry = entry.trim()
            if (trimmedEntry.isEmpty()) {
                return@firstOrNull false
            }

            if (looksLikeRegex(trimmedEntry)) {
                runCatching {
                    Regex(trimmedEntry, RegexOption.IGNORE_CASE).containsMatchIn(extractedText)
                }.getOrDefault(false)
            } else {
                extractedText.contains(other = trimmedEntry, ignoreCase = true)
            }
        }
    }

    private fun looksLikeRegex(entry: String): Boolean =
        entry.any { character -> character in RegexMetaCharacters }

    private fun recycleBitmap(bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    private fun OcrEngine.label(): String =
        when (this) {
            is MLKitOcrEngine -> "ML Kit"
            is FossOcrEngine -> "FOSS RapidOCR"
            else -> "OCR"
        }

    private companion object {
        val RegexMetaCharacters = setOf(
            '\\',
            '^',
            '$',
            '.',
            '|',
            '?',
            '*',
            '+',
            '(',
            ')',
            '[',
            ']',
            '{',
            '}',
        )
    }
}
