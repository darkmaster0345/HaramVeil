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

enum class TextRecognitionEngine(
    val storageValue: String,
    val displayName: String,
) {
    ML_KIT(
        storageValue = "ml_kit",
        displayName = "Google ML Kit",
    ),
    FOSS_ONNX(
        storageValue = "foss_onnx",
        displayName = "FOSS RapidOCR via ONNX",
    ),
    ;

    companion object {
        fun fromStorageValue(value: String?): TextRecognitionEngine? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class VisualModelOption(
    val storageValue: String,
    val displayName: String,
    val assetName: String,
    val inputSize: Int,
) {
    MODEL_320(
        storageValue = "model_320",
        displayName = "320",
        assetName = "320n.onnx",
        inputSize = 320,
    ),
    MODEL_640(
        storageValue = "model_640",
        displayName = "640",
        assetName = "640m.onnx",
        inputSize = 640,
    ),
    ;

    companion object {
        fun fromStorageValue(value: String?): VisualModelOption? =
            entries.firstOrNull { it.storageValue == value }
    }
}

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isHighRisk: Boolean,
)

data class BenchmarkResult(
    val model320LatencyMs: Long?,
    val model640LatencyMs: Long?,
    val supportedModel: VisualModelOption?,
    val latencyBudgetMs: Long,
    val errorMessage: String? = null,
)

data class StoredSecurityQuestion(
    val questionId: String,
    val answerHash: String,
)

data class SecurityQuestion(
    val id: String,
    val prompt: String,
)

object SecurityQuestionCatalog {
    val questions = listOf(
        SecurityQuestion(
            id = "karachi_childhood",
            prompt = "Did you grow up in Karachi?",
        ),
        SecurityQuestion(
            id = "mother_a",
            prompt = "Does your mother's first name start with the letter A?",
        ),
        SecurityQuestion(
            id = "school_before_six",
            prompt = "Did you attend school before age 6?",
        ),
        SecurityQuestion(
            id = "older_sibling",
            prompt = "Do you have an older sibling?",
        ),
        SecurityQuestion(
            id = "left_handed",
            prompt = "Are you left-handed?",
        ),
        SecurityQuestion(
            id = "first_pet",
            prompt = "Did your family ever keep a pet at home?",
        ),
        SecurityQuestion(
            id = "same_city",
            prompt = "Do you still live in the same city where you were born?",
        ),
        SecurityQuestion(
            id = "memorized_surah",
            prompt = "Did you memorize a surah before age 12?",
        ),
        SecurityQuestion(
            id = "first_bike",
            prompt = "Did you learn to ride a bicycle before age 10?",
        ),
        SecurityQuestion(
            id = "boarding_school",
            prompt = "Did you ever study at a boarding school or hostel?",
        ),
    )

    fun byId(id: String): SecurityQuestion? = questions.firstOrNull { it.id == id }
}

data class StoredOnboardingSnapshot(
    val onboardingComplete: Boolean,
    val benchmarkResult: BenchmarkResult?,
    val selectedTextEngine: TextRecognitionEngine?,
    val selectedVisualModel: VisualModelOption?,
    val mode1Enabled: Boolean,
    val mode2Enabled: Boolean,
    val mode3Enabled: Boolean,
    val modeConfigurationSaved: Boolean,
    val monitoredPackages: Set<String>,
    val appSelectionSaved: Boolean,
    val securityQuestions: List<StoredSecurityQuestion>,
    val securitySetupSaved: Boolean,
)

data class PermissionReviewState(
    val accessibilityGranted: Boolean,
    val overlayGranted: Boolean,
    val deviceAdminGranted: Boolean,
    val privateStorageReady: Boolean,
) {
    val allCorePermissionsGranted: Boolean
        get() = accessibilityGranted && overlayGranted && deviceAdminGranted
}
