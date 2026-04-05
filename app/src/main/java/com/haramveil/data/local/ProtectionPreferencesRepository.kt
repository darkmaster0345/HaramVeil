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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.haramveil.data.models.ProtectionSettings
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.data.models.VisualModelOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class ProtectionPreferencesRepository(
    private val context: Context,
) {
    val settingsFlow: Flow<ProtectionSettings> = context.haramVeilPreferencesDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            ProtectionSettings(
                monitoredPackages = preferences[HaramVeilPreferenceKeys.SelectedPackages] ?: emptySet(),
                keywordBlocklist = (preferences[HaramVeilPreferenceKeys.KeywordBlocklist]
                    ?: DefaultKeywordBlocklist.entries.toSet())
                    .sorted(),
                mode1Enabled = preferences[HaramVeilPreferenceKeys.Mode1Enabled] ?: true,
                mode2Enabled = preferences[HaramVeilPreferenceKeys.Mode2Enabled] ?: true,
                mode3Enabled = preferences[HaramVeilPreferenceKeys.Mode3Enabled] ?: false,
                selectedTextEngine = TextRecognitionEngine.fromStorageValue(
                    preferences[HaramVeilPreferenceKeys.SelectedTextEngine],
                ) ?: TextRecognitionEngine.ML_KIT,
                selectedVisualModel = VisualModelOption.fromStorageValue(
                    preferences[HaramVeilPreferenceKeys.SelectedVisualModel],
                ),
                frameSkipIntervalMs = preferences[HaramVeilPreferenceKeys.FrameSkipIntervalMs]
                    ?: HaramVeilPreferenceKeys.DefaultFrameSkipIntervalMs,
                mode3InferenceIntervalMs = preferences[HaramVeilPreferenceKeys.Mode3InferenceIntervalMs]
                    ?: HaramVeilPreferenceKeys.DefaultMode3InferenceIntervalMs,
                lockdownDurationMs = preferences[HaramVeilPreferenceKeys.LockdownDurationMs]
                    ?: HaramVeilPreferenceKeys.DefaultLockdownDurationMs,
                topCapturePercent = (preferences[HaramVeilPreferenceKeys.TopCapturePercent]
                    ?: HaramVeilPreferenceKeys.DefaultTopCapturePercent.toLong())
                    .toInt(),
                middleCapturePercent = (preferences[HaramVeilPreferenceKeys.MiddleCapturePercent]
                    ?: HaramVeilPreferenceKeys.DefaultMiddleCapturePercent.toLong())
                    .toInt(),
                accessibilitySettingsPromptShown = preferences[HaramVeilPreferenceKeys.AccessibilitySettingsPromptShown]
                    ?: false,
            )
        }

    suspend fun readSettings(): ProtectionSettings = settingsFlow.first()

    suspend fun saveMonitoredPackages(packageNames: Set<String>) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.SelectedPackages] = packageNames
            preferences[HaramVeilPreferenceKeys.AppSelectionSaved] = true
        }
    }

    suspend fun saveKeywordBlocklist(entries: List<String>) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            val sanitizedEntries = entries
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSet()
            preferences[HaramVeilPreferenceKeys.KeywordBlocklist] =
                if (sanitizedEntries.isEmpty()) {
                    DefaultKeywordBlocklist.entries.toSet()
                } else {
                    sanitizedEntries
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

    suspend fun saveFrameSkipIntervalMs(intervalMs: Long) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.FrameSkipIntervalMs] =
                intervalMs.coerceIn(minimumValue = 250L, maximumValue = 2_000L)
        }
    }

    suspend fun saveMode3InferenceIntervalMs(intervalMs: Long) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.Mode3InferenceIntervalMs] =
                intervalMs.coerceIn(minimumValue = 1_000L, maximumValue = 2_000L)
        }
    }

    suspend fun saveLockdownDurationMs(durationMs: Long) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.LockdownDurationMs] =
                durationMs.coerceIn(minimumValue = 5 * 60 * 1_000L, maximumValue = 24 * 60 * 60 * 1_000L)
        }
    }

    suspend fun saveHaramClipConfiguration(
        topCapturePercent: Int,
        middleCapturePercent: Int,
    ) {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.TopCapturePercent] =
                topCapturePercent.coerceIn(minimumValue = 10, maximumValue = 45).toLong()
            preferences[HaramVeilPreferenceKeys.MiddleCapturePercent] =
                middleCapturePercent.coerceIn(minimumValue = 20, maximumValue = 50).toLong()
        }
    }

    suspend fun markAccessibilitySettingsPromptShown() {
        context.haramVeilPreferencesDataStore.edit { preferences ->
            preferences[HaramVeilPreferenceKeys.AccessibilitySettingsPromptShown] = true
        }
    }
}

object DefaultKeywordBlocklist {
    val entries: List<String> = listOf(
        "nsfw",
        "porn",
        "sex",
        "onlyfans",
        "dating",
        "hookup",
        "escort",
        "nude",
        "(?i)cam\\s?girl",
        "(?i)adult\\s?video",
    )
}
