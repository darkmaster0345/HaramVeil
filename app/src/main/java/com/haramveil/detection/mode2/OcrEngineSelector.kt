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
import com.haramveil.data.local.ProtectionPreferencesRepository
import com.haramveil.data.models.ModelDownloadManager
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.utils.DispatcherProvider

class OcrEngineSelector(
    context: Context,
    private val protectionPreferencesRepository: ProtectionPreferencesRepository,
    dispatcherProvider: DispatcherProvider = DispatcherProvider(),
) {
    private val modelDownloadManager = ModelDownloadManager(context, dispatcherProvider)
    private val mlKitOcrEngine = MLKitOcrEngine(context, dispatcherProvider)
    private val fossOcrEngine = FossOcrEngine(
        context = context,
        modelDownloadManager = modelDownloadManager,
        dispatcherProvider = dispatcherProvider,
    )

    suspend fun select(
        preferredEngine: TextRecognitionEngine? = null,
    ): OcrEngine {
        val selectedEngine = preferredEngine ?: protectionPreferencesRepository.readSettings().selectedTextEngine
        return when (selectedEngine) {
            TextRecognitionEngine.FOSS_ONNX -> when {
                fossOcrEngine.isAvailable() -> fossOcrEngine
                mlKitOcrEngine.isAvailable() -> mlKitOcrEngine
                else -> fossOcrEngine
            }

            TextRecognitionEngine.ML_KIT -> when {
                mlKitOcrEngine.isAvailable() -> mlKitOcrEngine
                fossOcrEngine.isAvailable() -> fossOcrEngine
                else -> mlKitOcrEngine
            }
        }
    }

    fun close() {
        mlKitOcrEngine.close()
        fossOcrEngine.close()
    }
}
