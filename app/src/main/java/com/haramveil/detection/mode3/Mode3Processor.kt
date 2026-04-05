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

package com.haramveil.detection.mode3

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import android.os.SystemClock
import android.util.Log
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class Mode3Processor(
    context: Context,
    private val accessibilityServiceProvider: () -> AccessibilityService?,
    private val protectionPreferencesRepository: ProtectionPreferencesRepository,
    private val settingsProvider: () -> ProtectionSettings,
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) {
    private val processorScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)
    private val screenCaptureManager = ScreenCaptureManager(dispatcherProvider)
    private val statsRepository = StatsRepository.getInstance(context)
    private val modelSelector = ModelSelector(
        context = context,
        protectionPreferencesRepository = protectionPreferencesRepository,
    )
    private val preprocessor = NudeNetPreprocessor(dispatcherProvider)
    private val inferenceEngine = NudeNetInferenceEngine(
        context = context,
        dispatcherProvider = dispatcherProvider,
    )
    private var processorJob: Job? = null
    private var lastInferenceStartedAtMs = 0L
    private var consecutiveSlowInferences = 0
    private var thermalRateLimitOverrideMs: Long? = null

    fun start() {
        if (processorJob != null) {
            return
        }

        processorJob = processorScope.launch(dispatcherProvider.default) {
            runCatching {
                inferenceEngine.warm(modelSelector.select())
            }

            DetectionBus.events
                .filterIsInstance<DetectionEvent.Mode1Triggered>()
                .collect { event ->
                    processTrigger(event.trigger)
                }
        }
    }

    fun stop() {
        processorJob?.cancel()
        processorJob = null
        lastInferenceStartedAtMs = 0L
        consecutiveSlowInferences = 0
        thermalRateLimitOverrideMs = null
    }

    fun destroy() {
        stop()
        inferenceEngine.close()
        processorScope.cancel()
    }

    private suspend fun processTrigger(trigger: Mode1WakeRequest) {
        withContext(dispatcherProvider.default) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

            val settings = settingsProvider()
            if (!settings.mode3Enabled || !trigger.wakeMode3) {
                return@withContext
            }

            val activeRateLimitMs = thermalRateLimitOverrideMs
                ?: settings.mode3InferenceIntervalMs.coerceIn(minimumValue = 1_000L, maximumValue = 2_000L)
            val now = SystemClock.elapsedRealtime()
            if (now - lastInferenceStartedAtMs < activeRateLimitMs) {
                DetectionBus.publishMode3Clear(
                    packageName = trigger.scanResult.packageName,
                    details = "Visual scan skipped to respect the ${activeRateLimitMs}ms rate limit.",
                )
                return@withContext
            }
            lastInferenceStartedAtMs = now

            val accessibilityService = accessibilityServiceProvider() ?: run {
                DetectionBus.publishMode3Clear(
                    packageName = trigger.scanResult.packageName,
                    details = "Accessibility service instance was unavailable for visual scanning.",
                )
                return@withContext
            }

            val captureBitmap = screenCaptureManager.captureHaramClip(accessibilityService, settings) ?: run {
                DetectionBus.publishMode3Clear(
                    packageName = trigger.scanResult.packageName,
                    details = "Screenshot capture is unavailable on this device or Android version.",
                )
                return@withContext
            }

            try {
                val modelConfig = modelSelector.select()
                inferenceEngine.warm(modelConfig)
                val tensor = preprocessor.createTensor(
                    bitmap = captureBitmap,
                    modelConfig = modelConfig,
                    environment = inferenceEngine.environment(),
                )
                val onnxOutput = inferenceEngine.run(
                    tensor = tensor,
                    modelConfig = modelConfig,
                )
                val detectionResult = inferenceEngine.evaluate(onnxOutput)
                updateThermalGuard(
                    packageName = trigger.scanResult.packageName,
                    configuredRateLimitMs = settings.mode3InferenceIntervalMs,
                    inferenceTimeMs = onnxOutput.inferenceTimeMs,
                )

                val summaryDetails =
                    "Model ${modelConfig.visualModel.displayName} completed in ${onnxOutput.inferenceTimeMs}ms."

                if (detectionResult.isUnsafe) {
                    val matchDetails =
                        "$summaryDetails Unsafe class ${detectionResult.className} scored ${"%.2f".format(detectionResult.confidence)}."
                    statsRepository.logBlock(
                        packageName = trigger.scanResult.packageName,
                        mode = 3,
                        detail = "visual_content",
                        lockdownMs = settings.lockdownDurationMs,
                    )
                    DetectionBus.publishMode3Triggered(
                        packageName = trigger.scanResult.packageName,
                        matchDetails = matchDetails,
                    )
                    DetectionBus.publishVeilRequested(
                        packageName = trigger.scanResult.packageName,
                        triggerMode = DetectionTriggerMode.MODE_3,
                        matchDetails = matchDetails,
                    )
                    Log.w(LogTag, "Mode 3 unsafe detection for ${trigger.scanResult.packageName}. $matchDetails")
                } else {
                    DetectionBus.publishMode3Clear(
                        packageName = trigger.scanResult.packageName,
                        details = "$summaryDetails Top class ${detectionResult.className}.",
                    )
                    Log.i(LogTag, "Mode 3 clear result for ${trigger.scanResult.packageName}. $summaryDetails")
                }
            } finally {
                recycleBitmap(captureBitmap)
            }
        }
    }

    private suspend fun updateThermalGuard(
        packageName: String,
        configuredRateLimitMs: Long,
        inferenceTimeMs: Long,
    ) {
        if (inferenceTimeMs > SlowInferenceThresholdMs) {
            consecutiveSlowInferences += 1
        } else {
            consecutiveSlowInferences = 0
            thermalRateLimitOverrideMs = null
            return
        }

        if (consecutiveSlowInferences >= SlowInferenceStrikeCount) {
            val newRateLimitMs = max(configuredRateLimitMs, ThermalGuardRateLimitMs)
            thermalRateLimitOverrideMs = newRateLimitMs
            consecutiveSlowInferences = 0
            protectionPreferencesRepository.saveMode3InferenceIntervalMs(newRateLimitMs)
            val warningMessage =
                "Thermal guard raised visual scan pacing to ${newRateLimitMs}ms after repeated slow inferences in $packageName."
            DetectionBus.publishMode3Clear(
                packageName = packageName,
                details = warningMessage,
            )
            Log.w(LogTag, warningMessage)
        }
    }

    private fun recycleBitmap(bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    private companion object {
        const val LogTag = "HaramVeilMode3"
        const val SlowInferenceThresholdMs = 1_500L
        const val ThermalGuardRateLimitMs = 2_000L
        const val SlowInferenceStrikeCount = 3
    }
}
