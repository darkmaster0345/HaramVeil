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
import android.graphics.Bitmap
import com.haramveil.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

class NudeNetPreprocessor(
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) {

    suspend fun createTensor(
        bitmap: Bitmap,
        modelConfig: ModelConfig,
        environment: OrtEnvironment,
    ): OnnxTensor = withContext(dispatcherProvider.default) {
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            modelConfig.inputSize,
            modelConfig.inputSize,
            true,
        )

        try {
            val inputSize = modelConfig.inputSize
            val pixels = IntArray(inputSize * inputSize)
            scaledBitmap.getPixels(
                pixels,
                0,
                inputSize,
                0,
                0,
                inputSize,
                inputSize,
            )

            val channelSize = inputSize * inputSize
            val inputData = FloatArray(channelSize * 3)

            for (index in pixels.indices) {
                val pixel = pixels[index]
                inputData[index] = ((pixel shr 16) and 0xFF) / 255f
                inputData[channelSize + index] = ((pixel shr 8) and 0xFF) / 255f
                inputData[(channelSize * 2) + index] = (pixel and 0xFF) / 255f
            }

            OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(inputData),
                longArrayOf(1L, 3L, inputSize.toLong(), inputSize.toLong()),
            )
        } finally {
            if (!scaledBitmap.isRecycled && scaledBitmap !== bitmap) {
                scaledBitmap.recycle()
            }
        }
    }
}
