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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.haramveil.data.models.BenchmarkResult
import com.haramveil.data.models.StoredOnboardingSnapshot
import com.haramveil.data.models.StoredSecurityQuestion
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.data.models.VisualModelOption
import com.haramveil.service.ProtectionBootstrapStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

class OnboardingPreferencesRepository(
    private val context: Context,
) {

    suspend fun readSnapshot(): StoredOnboardingSnapshot {
        val preferences = context.haramVeilPreferencesDataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .first()

        val benchmark320Latency = preferences[HaramVeilPreferenceKeys.Benchmark320LatencyMs]
        val benchmark640Latency = preferences[HaramVeilPreferenceKeys.Benchmark640LatencyMs]
        val supportedModel = VisualModelOption.fromStorageValue(
            preferences[HaramVeilPreferenceKeys.SupportedVisualModel],
        )
        val benchmarkResult =
            if (benchmark320Latency != null || benchmark640Latency != null || supportedModel != null) {
                BenchmarkResult(
                    model320LatencyMs = benchmark320Latency,
                    model640LatencyMs = benchmark640Latency,
                    supportedModel = supportedModel,
                    latencyBudgetMs = preferences[HaramVeilPreferenceKeys.BenchmarkLatencyBudgetMs]
                        ?: HaramVeilPreferenceKeys.DefaultLatencyBudgetMs,
                    errorMessage = preferences[HaramVeilPreferenceKeys.BenchmarkErrorMessage],
                )
            } else {
                null
            }

        return StoredOnboardingSnapshot(
            onboardingComplete = preferences[HaramVeilPreferenceKeys.OnboardingComplete] ?: false,
            benchmarkResult = benchmarkResult,
            selectedTextEngine = TextRecognitionEngine.fromStorageValue(
                preferences[HaramVeilPreferenceKeys.SelectedTextEngine],
            ),
            selectedVisualModel = VisualModelOption.fromStorageValue(
                preferences[HaramVeilPreferenceKeys.SelectedVisualModel],
            ),
            mode1Enabled = preferences[HaramVeilPreferenceKeys.Mode1Enabled] ?: true,
            mode2Enabled = preferences[HaramVeilPreferenceKeys.Mode2Enabled] ?: true,
            mode3Enabled = preferences[HaramVeilPreferenceKeys.Mode3Enabled] ?: false,
            modeConfigurationSaved = preferences[HaramVeilPreferenceKeys.ModeConfigurationSaved] ?: false,
            monitoredPackages = preferences[HaramVeilPreferenceKeys.SelectedPackages] ?: emptySet(),
            appSelectionSaved = preferences[HaramVeilPreferenceKeys.AppSelectionSaved] ?: false,
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
            securitySetupSaved = preferences[HaramVeilPreferenceKeys.SecuritySetupSaved] ?: false,
        )
    }

    suspend fun saveBenchmarkResult(result: BenchmarkResult) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.BenchmarkLatencyBudgetMs] = result.latencyBudgetMs
            result.model320LatencyMs?.let { preferences[HaramVeilPreferenceKeys.Benchmark320LatencyMs] = it }
            result.model640LatencyMs?.let { preferences[HaramVeilPreferenceKeys.Benchmark640LatencyMs] = it }
            if (result.model320LatencyMs == null) {
                preferences.remove(HaramVeilPreferenceKeys.Benchmark320LatencyMs)
            }
            if (result.model640LatencyMs == null) {
                preferences.remove(HaramVeilPreferenceKeys.Benchmark640LatencyMs)
            }
            result.supportedModel?.let { preferences[HaramVeilPreferenceKeys.SupportedVisualModel] = it.storageValue }
            if (result.supportedModel == null) {
                preferences.remove(HaramVeilPreferenceKeys.SupportedVisualModel)
            }
            result.errorMessage?.let { preferences[HaramVeilPreferenceKeys.BenchmarkErrorMessage] = it }
            if (result.errorMessage == null) {
                preferences.remove(HaramVeilPreferenceKeys.BenchmarkErrorMessage)
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
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.SelectedTextEngine] = textEngine.storageValue
            preferences[HaramVeilPreferenceKeys.Mode1Enabled] = mode1Enabled
            preferences[HaramVeilPreferenceKeys.Mode2Enabled] = mode2Enabled
            preferences[HaramVeilPreferenceKeys.Mode3Enabled] = mode3Enabled
            preferences[HaramVeilPreferenceKeys.ModeConfigurationSaved] = true
            if (visualModel != null) {
                preferences[HaramVeilPreferenceKeys.SelectedVisualModel] = visualModel.storageValue
            } else {
                preferences.remove(HaramVeilPreferenceKeys.SelectedVisualModel)
            }
        }
    }

    suspend fun saveSelectedPackages(packageNames: Set<String>) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.SelectedPackages] = packageNames
            preferences[HaramVeilPreferenceKeys.AppSelectionSaved] = true
        }
    }

    suspend fun saveSecurityQuestions(questions: List<StoredSecurityQuestion>) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            for (slot in 1..3) {
                preferences.remove(questionKey(slot))
                preferences.remove(answerHashKey(slot))
            }
            questions.forEachIndexed { index, question ->
                val slot = index + 1
                preferences[questionKey(slot)] = question.questionId
                preferences[answerHashKey(slot)] = question.answerHash
            }
            preferences[HaramVeilPreferenceKeys.SecuritySetupSaved] = questions.size == 3
        }
    }

    suspend fun markOnboardingComplete(
        mode1Enabled: Boolean,
        mode2Enabled: Boolean,
        mode3Enabled: Boolean,
        textEngine: TextRecognitionEngine,
        visualModel: VisualModelOption?,
        monitoredPackages: Set<String>,
    ) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.OnboardingComplete] = true
            preferences[HaramVeilPreferenceKeys.ProtectionEnabled] = true
            // Re-save mode configuration atomically with the completion flag
            // so Samsung's aggressive process management cannot lose the earlier write.
            preferences[HaramVeilPreferenceKeys.Mode1Enabled] = mode1Enabled
            preferences[HaramVeilPreferenceKeys.Mode2Enabled] = mode2Enabled
            preferences[HaramVeilPreferenceKeys.Mode3Enabled] = mode3Enabled
            preferences[HaramVeilPreferenceKeys.SelectedTextEngine] = textEngine.storageValue
            preferences[HaramVeilPreferenceKeys.ModeConfigurationSaved] = true
            if (visualModel != null) {
                preferences[HaramVeilPreferenceKeys.SelectedVisualModel] = visualModel.storageValue
            }
            if (monitoredPackages.isNotEmpty()) {
                preferences[HaramVeilPreferenceKeys.SelectedPackages] = monitoredPackages
                preferences[HaramVeilPreferenceKeys.AppSelectionSaved] = true
            }
        }
        ProtectionBootstrapStore(context).markOnboardingComplete()
        ProtectionBootstrapStore(context).setProtectionEnabled(true)
    }

    private fun questionKey(slot: Int): Preferences.Key<String> =
        HaramVeilPreferenceKeys.securityQuestionKey(slot)

    private fun answerHashKey(slot: Int): Preferences.Key<String> =
        HaramVeilPreferenceKeys.securityAnswerHashKey(slot)
}
