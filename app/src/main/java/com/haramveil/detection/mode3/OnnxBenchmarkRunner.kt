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

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import com.haramveil.data.models.BenchmarkResult
import com.haramveil.data.models.VisualModelOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import kotlin.system.measureTimeMillis

class OnnxBenchmarkRunner(
    private val context: Context,
) {

    suspend fun run(): BenchmarkResult =
        withContext(Dispatchers.Default) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

            val latency320 = benchmarkModel(VisualModelOption.MODEL_320)
            val latency640 = benchmarkModel(VisualModelOption.MODEL_640)
            val supportedModel =
                when {
                    latency640 != null && latency640 <= LatencyBudgetMs -> VisualModelOption.MODEL_640
                    latency320 != null -> VisualModelOption.MODEL_320
                    else -> null
                }

            BenchmarkResult(
                model320LatencyMs = latency320,
                model640LatencyMs = latency640,
                supportedModel = supportedModel,
                latencyBudgetMs = LatencyBudgetMs,
                errorMessage = if (latency320 == null && latency640 == null) {
                    "Both ONNX benchmark passes failed on this device."
                } else {
                    null
                },
            )
        }

    private fun benchmarkModel(model: VisualModelOption): Long? =
        runCatching {
            val temporaryModel = copyAssetToCache(model.assetName)
            try {
                val environment = OrtEnvironment.getEnvironment()
                OrtSession.SessionOptions().use { sessionOptions ->
                    environment.createSession(temporaryModel.absolutePath, sessionOptions).use { session ->
                        val inputEntry = session.inputInfo.entries.first()
                        val tensorInfo = inputEntry.value.info as TensorInfo
                        val inputShape = tensorInfo.shape.mapIndexed { index, dimension ->
                            when {
                                dimension > 0L -> dimension
                                index == 0 -> 1L
                                else -> model.inputSize.toLong()
                            }
                        }.toLongArray()

                        val blankBitmap = Bitmap.createBitmap(
                            model.inputSize,
                            model.inputSize,
                            Bitmap.Config.ARGB_8888,
                        )
                        val inputData = FloatArray(inputShape.fold(1L) { total, dimension -> total * dimension }.toInt()) {
                            0f
                        }

                        blankBitmap.recycle()

                        OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputData), inputShape).use { tensor ->
                            measureTimeMillis {
                                session.run(mapOf(inputEntry.key to tensor)).use { _ -> }
                            }
                        }
                    }
                }
            } finally {
                temporaryModel.delete()
            }
        }.getOrNull()

    private fun copyAssetToCache(assetName: String): File {
        val cachedModelFile = File(context.cacheDir, assetName)
        context.assets.open(assetName).use { inputStream ->
            cachedModelFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return cachedModelFile
    }

    private companion object {
        const val LatencyBudgetMs = 1_500L
    }
}
