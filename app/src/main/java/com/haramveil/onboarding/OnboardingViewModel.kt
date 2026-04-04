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

package com.haramveil.onboarding

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haramveil.data.local.OnboardingPreferencesRepository
import com.haramveil.data.models.BenchmarkResult
import com.haramveil.data.models.InstalledAppInfo
import com.haramveil.data.models.SecurityQuestion
import com.haramveil.data.models.StoredSecurityQuestion
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.data.models.VisualModelOption
import com.haramveil.detection.mode3.OnnxBenchmarkRunner
import com.haramveil.security.PinSecurityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class BenchmarkUiState(
    val isRunning: Boolean = false,
    val model320LatencyMs: Long? = null,
    val model640LatencyMs: Long? = null,
    val supportedModel: VisualModelOption? = null,
    val latencyBudgetMs: Long = 1_500L,
    val errorMessage: String? = null,
)

data class OnboardingUiState(
    val isInitializing: Boolean = true,
    val onboardingComplete: Boolean = false,
    val benchmark: BenchmarkUiState = BenchmarkUiState(),
    val playServicesAvailable: Boolean = false,
    val isDeGoogledDevice: Boolean = false,
    val selectedTextEngine: TextRecognitionEngine = TextRecognitionEngine.ML_KIT,
    val selectedVisualModel: VisualModelOption? = null,
    val mode1Enabled: Boolean = true,
    val mode2Enabled: Boolean = true,
    val mode3Enabled: Boolean = false,
    val modeConfigurationSaved: Boolean = false,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val appSelectionSaved: Boolean = false,
    val hasPinConfigured: Boolean = false,
    val securitySetupSaved: Boolean = false,
) {
    val recommendedTextEngine: TextRecognitionEngine
        get() = if (isDeGoogledDevice) TextRecognitionEngine.FOSS_ONNX else TextRecognitionEngine.ML_KIT

    val enabledModesSummary: String
        get() = buildList {
            if (mode1Enabled) add("Mode 1")
            if (mode2Enabled) add("Mode 2")
            if (mode3Enabled) add("Mode 3")
        }.joinToString(separator = ", ")

    val monitoredAppsCount: Int
        get() = selectedPackages.size
}

class OnboardingViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val applicationContext = application.applicationContext
    private val repository = OnboardingPreferencesRepository(applicationContext)
    private val pinSecurityStore = PinSecurityStore(applicationContext)
    private val benchmarkRunner = OnnxBenchmarkRunner(applicationContext)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    fun toggleMode1(enabled: Boolean) {
        _uiState.update { current ->
            when {
                enabled -> current.copy(mode1Enabled = true)
                else -> current.copy(
                    mode1Enabled = false,
                    mode2Enabled = false,
                    mode3Enabled = false,
                )
            }
        }
    }

    fun toggleMode2(enabled: Boolean) {
        _uiState.update { current ->
            when {
                enabled -> current.copy(
                    mode1Enabled = true,
                    mode2Enabled = true,
                )
                else -> current.copy(
                    mode2Enabled = false,
                    mode3Enabled = false,
                )
            }
        }
    }

    fun toggleMode3(enabled: Boolean) {
        _uiState.update { current ->
            when {
                enabled && current.benchmark.supportedModel != null -> current.copy(
                    mode1Enabled = true,
                    mode2Enabled = true,
                    mode3Enabled = true,
                    selectedVisualModel = current.selectedVisualModel ?: current.benchmark.supportedModel,
                )
                else -> current.copy(mode3Enabled = false)
            }
        }
    }

    fun selectTextEngine(engine: TextRecognitionEngine) {
        _uiState.update { current ->
            current.copy(selectedTextEngine = engine)
        }
    }

    fun selectVisualModel(model: VisualModelOption) {
        _uiState.update { current ->
            current.copy(selectedVisualModel = model)
        }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { current ->
            val selectedPackages =
                if (current.selectedPackages.contains(packageName)) {
                    current.selectedPackages - packageName
                } else {
                    current.selectedPackages + packageName
                }
            current.copy(selectedPackages = selectedPackages)
        }
    }

    fun selectAllApps() {
        _uiState.update { current ->
            current.copy(selectedPackages = current.installedApps.map { it.packageName }.toSet())
        }
    }

    fun deselectAllApps() {
        _uiState.update { current ->
            current.copy(selectedPackages = emptySet())
        }
    }

    suspend fun persistModeConfiguration() {
        val state = _uiState.value
        repository.saveModeConfiguration(
            textEngine = state.selectedTextEngine,
            visualModel = state.selectedVisualModel,
            mode1Enabled = state.mode1Enabled,
            mode2Enabled = state.mode2Enabled,
            mode3Enabled = state.mode3Enabled,
        )
        _uiState.update { current ->
            current.copy(modeConfigurationSaved = true)
        }
    }

    suspend fun persistSelectedApps() {
        val packages = _uiState.value.selectedPackages
        repository.saveSelectedPackages(packages)
        _uiState.update { current ->
            current.copy(appSelectionSaved = true)
        }
    }

    suspend fun persistPinAndSecuritySetup(
        pin: String,
        securityAnswers: List<Pair<SecurityQuestion, Boolean>>,
    ) {
        withContext(Dispatchers.IO) {
            pinSecurityStore.storePin(pin)
        }
        repository.saveSecurityQuestions(
            questions = securityAnswers.map { (question, answer) ->
                StoredSecurityQuestion(
                    questionId = question.id,
                    answerHash = sha256Hash("${question.id}:${answer.toString().lowercase()}"),
                )
            },
        )
        _uiState.update { current ->
            current.copy(
                hasPinConfigured = true,
                securitySetupSaved = true,
            )
        }
    }

    suspend fun markOnboardingComplete() {
        repository.markOnboardingComplete()
        _uiState.update { current ->
            current.copy(onboardingComplete = true)
        }
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val snapshot = repository.readSnapshot()
            val playServicesAvailable = isPackageInstalled("com.google.android.gms")
            val installedApps = loadInstalledApps()
            val benchmarkState = snapshot.benchmarkResult.toUiState(
                isRunning = snapshot.benchmarkResult == null,
            )

            val supportedModel = snapshot.benchmarkResult?.supportedModel
            val defaultVisualModel = snapshot.selectedVisualModel ?: supportedModel
            val defaultTextEngine = snapshot.selectedTextEngine ?: when {
                playServicesAvailable -> TextRecognitionEngine.ML_KIT
                else -> TextRecognitionEngine.FOSS_ONNX
            }
            val normalizedModes = normalizeModeSelection(
                mode1Enabled = if (snapshot.modeConfigurationSaved) snapshot.mode1Enabled else true,
                mode2Enabled = if (snapshot.modeConfigurationSaved) snapshot.mode2Enabled else true,
                mode3Enabled = if (snapshot.modeConfigurationSaved) snapshot.mode3Enabled else supportedModel != null,
                supportedVisualModel = supportedModel,
            )
            val selectedPackages =
                if (snapshot.appSelectionSaved) {
                    snapshot.monitoredPackages
                } else {
                    installedApps.filter { it.isHighRisk }
                        .map { it.packageName }
                        .toSet()
                }

            _uiState.value = OnboardingUiState(
                isInitializing = false,
                onboardingComplete = snapshot.onboardingComplete,
                benchmark = benchmarkState,
                playServicesAvailable = playServicesAvailable,
                isDeGoogledDevice = !playServicesAvailable,
                selectedTextEngine = defaultTextEngine,
                selectedVisualModel = defaultVisualModel,
                mode1Enabled = normalizedModes.mode1Enabled,
                mode2Enabled = normalizedModes.mode2Enabled,
                mode3Enabled = normalizedModes.mode3Enabled,
                modeConfigurationSaved = snapshot.modeConfigurationSaved,
                installedApps = installedApps,
                selectedPackages = selectedPackages,
                appSelectionSaved = snapshot.appSelectionSaved,
                hasPinConfigured = pinSecurityStore.hasPin(),
                securitySetupSaved = snapshot.securitySetupSaved,
            )

            if (snapshot.benchmarkResult == null) {
                runBenchmark()
            }
        }
    }

    private suspend fun runBenchmark() {
        _uiState.update { current ->
            current.copy(
                benchmark = current.benchmark.copy(
                    isRunning = true,
                    errorMessage = null,
                ),
            )
        }

        val result = benchmarkRunner.run()
        repository.saveBenchmarkResult(result)

        _uiState.update { current ->
            val selectedVisualModel =
                if (current.modeConfigurationSaved) {
                    current.selectedVisualModel
                } else {
                    current.selectedVisualModel ?: result.supportedModel
                }
            val normalizedModes =
                if (current.modeConfigurationSaved) {
                    ModeSelection(
                        mode1Enabled = current.mode1Enabled,
                        mode2Enabled = current.mode2Enabled,
                        mode3Enabled = current.mode3Enabled,
                    )
                } else {
                    normalizeModeSelection(
                        mode1Enabled = true,
                        mode2Enabled = true,
                        mode3Enabled = result.supportedModel != null,
                        supportedVisualModel = result.supportedModel,
                    )
                }

            current.copy(
                benchmark = result.toUiState(isRunning = false),
                selectedVisualModel = selectedVisualModel,
                mode1Enabled = normalizedModes.mode1Enabled,
                mode2Enabled = normalizedModes.mode2Enabled,
                mode3Enabled = normalizedModes.mode3Enabled,
            )
        }
    }

    private suspend fun loadInstalledApps(): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            val packageManager = applicationContext.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                .distinctBy { it.activityInfo.packageName }
                .map { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    InstalledAppInfo(
                        packageName = packageName,
                        label = resolveInfo.loadLabel(packageManager).toString(),
                        isHighRisk = isHighRiskPackage(packageName),
                    )
                }
                .sortedWith(
                    compareByDescending<InstalledAppInfo> { it.isHighRisk }
                        .thenBy { it.label.lowercase() },
                )
        }

    private fun isPackageInstalled(packageName: String): Boolean =
        runCatching {
            @Suppress("DEPRECATION")
            applicationContext.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrElse { false }

    private fun isHighRiskPackage(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        val exactHighRiskPackages = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "com.snapchat.android",
            "com.twitter.android",
            "com.xcorp.android",
            "com.google.android.youtube",
            "com.reddit.frontpage",
            "com.opera.browser",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "org.telegram.messenger",
            "com.whatsapp",
        )
        val keywordMatches = listOf(
            "browser",
            "video",
            "instagram",
            "tiktok",
            "twitter",
            "xcorp",
            "youtube",
            "reddit",
            "opera",
            "brave",
            "duckduckgo",
            "telegram",
            "whatsapp",
            "chrome",
            "firefox",
            "samsunginternet",
            "snapchat",
        )

        return normalized in exactHighRiskPackages || keywordMatches.any { normalized.contains(it) }
    }

    private fun normalizeModeSelection(
        mode1Enabled: Boolean,
        mode2Enabled: Boolean,
        mode3Enabled: Boolean,
        supportedVisualModel: VisualModelOption?,
    ): ModeSelection {
        val normalizedMode3 = mode3Enabled && supportedVisualModel != null
        val normalizedMode2 = mode2Enabled || normalizedMode3
        val normalizedMode1 = mode1Enabled || normalizedMode2
        return ModeSelection(
            mode1Enabled = normalizedMode1,
            mode2Enabled = normalizedMode2,
            mode3Enabled = normalizedMode3,
        )
    }

    private fun BenchmarkResult?.toUiState(isRunning: Boolean): BenchmarkUiState =
        if (this == null) {
            BenchmarkUiState(isRunning = isRunning)
        } else {
            BenchmarkUiState(
                isRunning = isRunning,
                model320LatencyMs = model320LatencyMs,
                model640LatencyMs = model640LatencyMs,
                supportedModel = supportedModel,
                latencyBudgetMs = latencyBudgetMs,
                errorMessage = errorMessage,
            )
        }

    private fun sha256Hash(rawValue: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawValue.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private data class ModeSelection(
        val mode1Enabled: Boolean,
        val mode2Enabled: Boolean,
        val mode3Enabled: Boolean,
    )
}
