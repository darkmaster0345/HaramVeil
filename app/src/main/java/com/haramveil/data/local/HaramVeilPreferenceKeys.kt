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

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object HaramVeilPreferenceKeys {
    const val DefaultLatencyBudgetMs = 1_500L
    const val DefaultFrameSkipIntervalMs = 500L
    const val DefaultMode3InferenceIntervalMs = 1_000L
    const val DefaultLockdownDurationMs = 15 * 60 * 1_000L
    const val DefaultTopCapturePercent = 30
    const val DefaultMiddleCapturePercent = 40
    const val DefaultStatsRetentionDays = 90

    val OnboardingComplete = booleanPreferencesKey("onboarding_complete")
    val ProtectionEnabled = booleanPreferencesKey("protection_enabled")
    val Benchmark320LatencyMs = longPreferencesKey("benchmark_320_latency_ms")
    val Benchmark640LatencyMs = longPreferencesKey("benchmark_640_latency_ms")
    val BenchmarkLatencyBudgetMs = longPreferencesKey("benchmark_latency_budget_ms")
    val BenchmarkErrorMessage = stringPreferencesKey("benchmark_error_message")
    val SupportedVisualModel = stringPreferencesKey("supported_visual_model")
    val SelectedVisualModel = stringPreferencesKey("selected_visual_model")
    val SelectedTextEngine = stringPreferencesKey("selected_text_engine")
    val Mode1Enabled = booleanPreferencesKey("mode_1_enabled")
    val Mode2Enabled = booleanPreferencesKey("mode_2_enabled")
    val Mode3Enabled = booleanPreferencesKey("mode_3_enabled")
    val ModeConfigurationSaved = booleanPreferencesKey("mode_configuration_saved")
    val SelectedPackages = stringSetPreferencesKey("selected_monitored_packages")
    val AppSelectionSaved = booleanPreferencesKey("app_selection_saved")
    val SecuritySetupSaved = booleanPreferencesKey("security_setup_saved")
    val KeywordBlocklist = stringSetPreferencesKey("keyword_blocklist")
    val FrameSkipIntervalMs = longPreferencesKey("frame_skip_interval_ms")
    val Mode3InferenceIntervalMs = longPreferencesKey("mode3_inference_interval_ms")
    val LockdownDurationMs = longPreferencesKey("lockdown_duration_ms")
    val StatsRetentionDays = longPreferencesKey("stats_retention_days")
    val TopCapturePercent = longPreferencesKey("top_capture_percent")
    val MiddleCapturePercent = longPreferencesKey("middle_capture_percent")
    val AccessibilitySettingsPromptShown = booleanPreferencesKey("accessibility_prompt_shown")
    val BatteryOptimizationPromptShown = booleanPreferencesKey("battery_optimization_prompt_shown")
    val BatteryOptimizationCompleted = booleanPreferencesKey("battery_optimization_completed")

    fun securityQuestionKey(slot: Int): Preferences.Key<String> =
        stringPreferencesKey("security_question_${slot}_id")

    fun securityAnswerHashKey(slot: Int): Preferences.Key<String> =
        stringPreferencesKey("security_question_${slot}_answer_hash")
}
