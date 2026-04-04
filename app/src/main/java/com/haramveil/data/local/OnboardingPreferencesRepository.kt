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

package com.haramveil.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.haramveil.data.models.BenchmarkResult
import com.haramveil.data.models.StoredOnboardingSnapshot
import com.haramveil.data.models.StoredSecurityQuestion
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.data.models.VisualModelOption
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.onboardingDataStore by preferencesDataStore(name = "haramveil_onboarding")

class OnboardingPreferencesRepository(
    private val context: Context,
) {

    suspend fun readSnapshot(): StoredOnboardingSnapshot {
        val preferences = context.onboardingDataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .first()

        val benchmark320Latency = preferences[Benchmark320LatencyKey]
        val benchmark640Latency = preferences[Benchmark640LatencyKey]
        val supportedModel = VisualModelOption.fromStorageValue(preferences[SupportedVisualModelKey])
        val benchmarkResult =
            if (benchmark320Latency != null || benchmark640Latency != null || supportedModel != null) {
                BenchmarkResult(
                    model320LatencyMs = benchmark320Latency,
                    model640LatencyMs = benchmark640Latency,
                    supportedModel = supportedModel,
                    latencyBudgetMs = preferences[BenchmarkLatencyBudgetKey] ?: DefaultLatencyBudgetMs,
                    errorMessage = preferences[BenchmarkErrorKey],
                )
            } else {
                null
            }

        return StoredOnboardingSnapshot(
            onboardingComplete = preferences[OnboardingCompleteKey] ?: false,
            benchmarkResult = benchmarkResult,
            selectedTextEngine = TextRecognitionEngine.fromStorageValue(preferences[TextEngineKey]),
            selectedVisualModel = VisualModelOption.fromStorageValue(preferences[SelectedVisualModelKey]),
            mode1Enabled = preferences[Mode1EnabledKey] ?: true,
            mode2Enabled = preferences[Mode2EnabledKey] ?: true,
            mode3Enabled = preferences[Mode3EnabledKey] ?: false,
            modeConfigurationSaved = preferences[ModeConfigurationSavedKey] ?: false,
            monitoredPackages = preferences[SelectedPackagesKey] ?: emptySet(),
            appSelectionSaved = preferences[AppSelectionSavedKey] ?: false,
            securityQuestions = buildList {
                for (slot in 1..3) {
                    val questionId = preferences[questionKey(slot)]
                    val answerHash = preferences[answerHashKey(slot)]
                    if (questionId != null && answerHash != null) {
                        add(
                            StoredSecurityQuestion(
                                questionId = questionId,
                                answerHash = answerHash,
                            ),
                        )
                    }
                }
            },
            securitySetupSaved = preferences[SecuritySetupSavedKey] ?: false,
        )
    }

    suspend fun saveBenchmarkResult(result: BenchmarkResult) {
        context.onboardingDataStore.edit { preferences ->
            preferences[BenchmarkLatencyBudgetKey] = result.latencyBudgetMs
            result.model320LatencyMs?.let { preferences[Benchmark320LatencyKey] = it }
            result.model640LatencyMs?.let { preferences[Benchmark640LatencyKey] = it }
            if (result.model320LatencyMs == null) {
                preferences.remove(Benchmark320LatencyKey)
            }
            if (result.model640LatencyMs == null) {
                preferences.remove(Benchmark640LatencyKey)
            }
            result.supportedModel?.let { preferences[SupportedVisualModelKey] = it.storageValue }
            if (result.supportedModel == null) {
                preferences.remove(SupportedVisualModelKey)
            }
            result.errorMessage?.let { preferences[BenchmarkErrorKey] = it }
            if (result.errorMessage == null) {
                preferences.remove(BenchmarkErrorKey)
            }
        }
    }

    suspend fun saveModeConfiguration(
        textEngine: TextRecognitionEngine,
        visualModel: VisualModelOption?,
        mode1Enabled: Boolean,
        mode2Enabled: Boolean,
        mode3Enabled: Boolean,
    ) {
        context.onboardingDataStore.edit { preferences ->
            preferences[TextEngineKey] = textEngine.storageValue
            preferences[Mode1EnabledKey] = mode1Enabled
            preferences[Mode2EnabledKey] = mode2Enabled
            preferences[Mode3EnabledKey] = mode3Enabled
            preferences[ModeConfigurationSavedKey] = true
            if (visualModel != null) {
                preferences[SelectedVisualModelKey] = visualModel.storageValue
            } else {
                preferences.remove(SelectedVisualModelKey)
            }
        }
    }

    suspend fun saveSelectedPackages(packageNames: Set<String>) {
        context.onboardingDataStore.edit { preferences ->
            preferences[SelectedPackagesKey] = packageNames
            preferences[AppSelectionSavedKey] = true
        }
    }

    suspend fun saveSecurityQuestions(questions: List<StoredSecurityQuestion>) {
        context.onboardingDataStore.edit { preferences ->
            for (slot in 1..3) {
                preferences.remove(questionKey(slot))
                preferences.remove(answerHashKey(slot))
            }
            questions.forEachIndexed { index, question ->
                val slot = index + 1
                preferences[questionKey(slot)] = question.questionId
                preferences[answerHashKey(slot)] = question.answerHash
            }
            preferences[SecuritySetupSavedKey] = questions.size == 3
        }
    }

    suspend fun markOnboardingComplete() {
        context.onboardingDataStore.edit { preferences ->
            preferences[OnboardingCompleteKey] = true
        }
    }

    private fun questionKey(slot: Int): Preferences.Key<String> =
        stringPreferencesKey("security_question_${slot}_id")

    private fun answerHashKey(slot: Int): Preferences.Key<String> =
        stringPreferencesKey("security_question_${slot}_answer_hash")

    private companion object {
        const val DefaultLatencyBudgetMs = 1_500L

        val OnboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
        val Benchmark320LatencyKey = longPreferencesKey("benchmark_320_latency_ms")
        val Benchmark640LatencyKey = longPreferencesKey("benchmark_640_latency_ms")
        val BenchmarkLatencyBudgetKey = longPreferencesKey("benchmark_latency_budget_ms")
        val BenchmarkErrorKey = stringPreferencesKey("benchmark_error_message")
        val SupportedVisualModelKey = stringPreferencesKey("supported_visual_model")
        val SelectedVisualModelKey = stringPreferencesKey("selected_visual_model")
        val TextEngineKey = stringPreferencesKey("selected_text_engine")
        val Mode1EnabledKey = booleanPreferencesKey("mode_1_enabled")
        val Mode2EnabledKey = booleanPreferencesKey("mode_2_enabled")
        val Mode3EnabledKey = booleanPreferencesKey("mode_3_enabled")
        val ModeConfigurationSavedKey = booleanPreferencesKey("mode_configuration_saved")
        val SelectedPackagesKey = stringSetPreferencesKey("selected_monitored_packages")
        val AppSelectionSavedKey = booleanPreferencesKey("app_selection_saved")
        val SecuritySetupSavedKey = booleanPreferencesKey("security_setup_saved")
    }
}
