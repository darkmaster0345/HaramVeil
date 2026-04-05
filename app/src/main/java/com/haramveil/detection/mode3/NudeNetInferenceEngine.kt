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
import android.os.SystemClock
import com.haramveil.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.exp
import kotlin.math.max

data class OnnxOutput(
    val classScores: Map<String, Float>,
    val inferenceTimeMs: Long,
)

data class DetectionResult(
    val isUnsafe: Boolean,
    val confidence: Float,
    val className: String,
    val inferenceTimeMs: Long,
)

class NudeNetInferenceEngine(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) {
    private val environment = OrtEnvironment.getEnvironment()
    private val sessionLock = Any()

    @Volatile
    private var activeSession: OrtSession? = null

    @Volatile
    private var activeModelConfig: ModelConfig? = null

    @Volatile
    private var cachedModelFile: File? = null

    suspend fun warm(modelConfig: ModelConfig) {
        withContext(dispatcherProvider.default) {
            ensureSession(modelConfig)
        }
    }

    suspend fun run(
        tensor: OnnxTensor,
        modelConfig: ModelConfig,
    ): OnnxOutput = withContext(dispatcherProvider.default) {
        val session = ensureSession(modelConfig)
        val inputEntry = session.inputInfo.entries.first()
        val outputInfo = session.outputInfo.entries.firstOrNull()?.value?.info as? TensorInfo
        val startedAt = SystemClock.elapsedRealtime()

        tensor.use { inputTensor ->
            session.run(mapOf(inputEntry.key to inputTensor)).use { outputs ->
                val firstOutput = outputs.firstOrNull()?.value
                val resolvedLabels = resolveLabels(
                    session = session,
                    outputInfo = outputInfo,
                    rawOutput = firstOutput,
                )

                OnnxOutput(
                    classScores = parseOutputScores(
                        rawOutput = firstOutput,
                        labels = resolvedLabels,
                    ),
                    inferenceTimeMs = SystemClock.elapsedRealtime() - startedAt,
                )
            }
        }
    }

    fun evaluate(
        output: OnnxOutput,
        threshold: Float = DefaultUnsafeThreshold,
    ): DetectionResult {
        val unsafeCandidate = output.classScores
            .filterKeys(::isUnsafeClassLabel)
            .maxByOrNull { it.value }

        return if (unsafeCandidate != null && unsafeCandidate.value >= threshold) {
            DetectionResult(
                isUnsafe = true,
                confidence = unsafeCandidate.value,
                className = unsafeCandidate.key,
                inferenceTimeMs = output.inferenceTimeMs,
            )
        } else {
            val topScore = output.classScores.maxByOrNull { it.value }
            DetectionResult(
                isUnsafe = false,
                confidence = topScore?.value ?: 0f,
                className = topScore?.key ?: "SAFE",
                inferenceTimeMs = output.inferenceTimeMs,
            )
        }
    }

    fun environment(): OrtEnvironment = environment

    fun close() {
        synchronized(sessionLock) {
            activeSession?.close()
            activeSession = null
            activeModelConfig = null
            cachedModelFile?.delete()
            cachedModelFile = null
        }
    }

    private fun ensureSession(modelConfig: ModelConfig): OrtSession {
        activeSession?.let { session ->
            if (activeModelConfig == modelConfig) {
                return session
            }
        }

        synchronized(sessionLock) {
            activeSession?.let { session ->
                if (activeModelConfig == modelConfig) {
                    return session
                }
            }

            activeSession?.close()
            cachedModelFile?.delete()
            val modelFile = copyAssetToCache(modelConfig.assetPath)
            val session = OrtSession.SessionOptions().use { sessionOptions ->
                sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                environment.createSession(modelFile.absolutePath, sessionOptions)
            }

            cachedModelFile = modelFile
            activeModelConfig = modelConfig
            activeSession = session
            return session
        }
    }

    private fun copyAssetToCache(assetName: String): File {
        val cachedFile = File(context.cacheDir, assetName)
        context.assets.open(assetName).use { inputStream ->
            cachedFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return cachedFile
    }

    private fun resolveLabels(
        session: OrtSession,
        outputInfo: TensorInfo?,
        rawOutput: Any?,
    ): List<String> {
        resolveMetadataLabels(session)?.let { labels ->
            if (labels.isNotEmpty()) {
                return labels
            }
        }

        val vectorLength = inferLabelCountFromOutput(outputInfo, rawOutput)
        return when {
            vectorLength == 2 -> listOf("SAFE", "UNSAFE")
            vectorLength in 1..DefaultNudeNetLabels.size -> DefaultNudeNetLabels.take(vectorLength)
            vectorLength > 0 -> List(vectorLength) { index -> "UNSAFE_CLASS_${index + 1}" }
            else -> DefaultNudeNetLabels
        }
    }

    private fun resolveMetadataLabels(session: OrtSession): List<String>? {
        val metadata = runCatching {
            session.javaClass.getMethod("getMetadata").invoke(session)
        }.getOrNull() ?: return null

        val customMetadata = sequenceOf("getCustomMetadata", "getCustomMetadataMap")
            .mapNotNull { methodName ->
                runCatching {
                    metadata.javaClass.getMethod(methodName).invoke(metadata) as? Map<*, *>
                }.getOrNull()
            }
            .firstOrNull()
            ?: return null

        val rawLabels = sequenceOf("classes", "labels", "names", "label_map")
            .mapNotNull { key -> customMetadata[key]?.toString() }
            .firstOrNull()
            ?: return null

        return rawLabels
            .replace("[", "")
            .replace("]", "")
            .lineSequence()
            .flatMap { line ->
                when {
                    line.contains(',') -> line.split(',').asSequence()
                    else -> sequenceOf(line)
                }
            }
            .map { token -> token.trim().trim('"', '\'') }
            .filter(String::isNotBlank)
            .toList()
            .ifEmpty { null }
    }

    private fun inferLabelCountFromOutput(
        outputInfo: TensorInfo?,
        rawOutput: Any?,
    ): Int {
        val outputShape = outputInfo?.shape
            ?.toList()
            ?.filter { dimension -> dimension > 0L }
            ?: emptyList()
        outputShape.firstOrNull { dimension -> dimension in 1L..64L }?.let { smallDimension ->
            return smallDimension.toInt()
        }

        return when (rawOutput) {
            is FloatArray -> rawOutput.size
            is Array<*> -> rawOutput.firstOrNull()?.let { inferLabelCountFromOutput(null, it) } ?: 0
            else -> 0
        }
    }

    private fun parseOutputScores(
        rawOutput: Any?,
        labels: List<String>,
    ): Map<String, Float> {
        return when (rawOutput) {
            is FloatArray -> mapScoreVector(rawOutput, labels)
            is Array<*> -> parseNestedOutput(rawOutput, labels)
            is Map<*, *> -> rawOutput.entries
                .mapNotNull { entry ->
                    val label = entry.key?.toString() ?: return@mapNotNull null
                    val score = (entry.value as? Number)?.toFloat() ?: return@mapNotNull null
                    label to normalizeScore(score)
                }
                .toMap()

            else -> emptyMap()
        }
    }

    private fun parseNestedOutput(
        rawOutput: Array<*>,
        labels: List<String>,
    ): Map<String, Float> {
        val floatRows = rawOutput.filterIsInstance<FloatArray>()
        if (floatRows.size == rawOutput.size && floatRows.isNotEmpty()) {
            return parse2dOutput(floatRows, labels)
        }

        val nestedArrays = rawOutput.filterIsInstance<Array<*>>()
        if (nestedArrays.isNotEmpty()) {
            return parseNestedOutput(nestedArrays.first(), labels)
        }

        return emptyMap()
    }

    private fun parse2dOutput(
        rows: List<FloatArray>,
        labels: List<String>,
    ): Map<String, Float> {
        if (rows.size == 1) {
            return mapScoreVector(rows.first(), labels)
        }

        val rowLength = rows.firstOrNull()?.size ?: return emptyMap()
        val likelyByColumns = rows.size in 6..64 && rowLength > 64
        val likelyByRows = rows.size > 64 && rowLength in 6..64

        return when {
            likelyByColumns -> parseDetectionColumns(rows, labels)
            likelyByRows -> parseDetectionRows(rows, labels)
            rowLength == labels.size || rowLength == labels.size + 1 -> {
                rows.fold(emptyMap()) { accumulator, row ->
                    mergeScores(
                        accumulator,
                        mapScoreVector(row.take(labels.size).toFloatArray(), labels),
                    )
                }
            }

            else -> mapScoreVector(
                rows.flatMap { row -> row.toList() }.take(labels.size).toFloatArray(),
                labels,
            )
        }
    }

    private fun parseDetectionRows(
        detections: List<FloatArray>,
        labels: List<String>,
    ): Map<String, Float> {
        val scores = mutableMapOf<String, Float>()
        detections.forEach { detection ->
            val classOffset = when {
                detection.size >= labels.size + 5 -> 5
                detection.size >= labels.size + 4 -> 4
                else -> return@forEach
            }
            val objectness = detection.getOrNull(4)
                ?.takeIf { detection.size >= labels.size + 5 }
                ?.let(::normalizeScore)
                ?: 1f
            val classCount = minOf(labels.size, detection.size - classOffset)

            for (classIndex in 0 until classCount) {
                val label = labels[classIndex]
                val score = normalizeScore(detection[classOffset + classIndex]) * objectness
                scores[label] = max(scores[label] ?: 0f, score)
            }
        }
        return scores
    }

    private fun parseDetectionColumns(
        featuresByDetection: List<FloatArray>,
        labels: List<String>,
    ): Map<String, Float> {
        val detectionCount = featuresByDetection.firstOrNull()?.size ?: return emptyMap()
        val classOffset = when {
            featuresByDetection.size >= labels.size + 5 -> 5
            featuresByDetection.size >= labels.size + 4 -> 4
            else -> return emptyMap()
        }
        val scores = mutableMapOf<String, Float>()
        val objectnessRow = if (featuresByDetection.size >= labels.size + 5) {
            featuresByDetection.getOrNull(4)
        } else {
            null
        }
        val classCount = minOf(labels.size, featuresByDetection.size - classOffset)

        for (detectionIndex in 0 until detectionCount) {
            val objectness = objectnessRow
                ?.getOrNull(detectionIndex)
                ?.let(::normalizeScore)
                ?: 1f

            for (classIndex in 0 until classCount) {
                val label = labels[classIndex]
                val score = normalizeScore(
                    featuresByDetection[classOffset + classIndex][detectionIndex],
                ) * objectness
                scores[label] = max(scores[label] ?: 0f, score)
            }
        }
        return scores
    }

    private fun mapScoreVector(
        scores: FloatArray,
        labels: List<String>,
    ): Map<String, Float> =
        buildMap {
            scores.forEachIndexed { index, score ->
                val label = labels.getOrNull(index) ?: "UNSAFE_CLASS_${index + 1}"
                put(label, normalizeScore(score))
            }
        }

    private fun mergeScores(
        left: Map<String, Float>,
        right: Map<String, Float>,
    ): Map<String, Float> =
        (left.keys + right.keys).associateWith { label ->
            max(left[label] ?: 0f, right[label] ?: 0f)
        }

    private fun normalizeScore(rawScore: Float): Float {
        return if (rawScore in 0f..1f) {
            rawScore
        } else {
            (1.0 / (1.0 + exp(-rawScore.toDouble()))).toFloat()
        }
    }

    private fun isUnsafeClassLabel(label: String): Boolean {
        val normalized = label.lowercase()
        return UnsafeClassMarkers.any { marker -> marker in normalized }
    }

    private companion object {
        const val DefaultUnsafeThreshold = 0.6f

        val UnsafeClassMarkers = setOf(
            "unsafe",
            "explicit",
            "exposed",
            "genitalia",
            "breast",
            "buttocks",
            "anus",
        )

        val DefaultNudeNetLabels = listOf(
            "FEMALE_GENITALIA_COVERED",
            "FACE_FEMALE",
            "BUTTOCKS_EXPOSED",
            "FEMALE_BREAST_EXPOSED",
            "FEMALE_GENITALIA_EXPOSED",
            "MALE_BREAST_EXPOSED",
            "ANUS_EXPOSED",
            "FEET_EXPOSED",
            "BELLY_COVERED",
            "FEET_COVERED",
            "ARMPITS_COVERED",
            "ARMPITS_EXPOSED",
            "FACE_MALE",
            "BELLY_EXPOSED",
            "MALE_GENITALIA_EXPOSED",
            "ANUS_COVERED",
            "FEMALE_BREAST_COVERED",
            "BUTTOCKS_COVERED",
        )
    }
}
