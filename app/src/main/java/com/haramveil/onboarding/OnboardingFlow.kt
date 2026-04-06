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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.haramveil.onboarding

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.haramveil.accessibility.isHaramVeilAccessibilityServiceEnabled
import com.haramveil.accessibility.openHaramVeilAccessibilitySettings
import com.haramveil.data.models.InstalledAppInfo
import com.haramveil.data.models.PermissionReviewState
import com.haramveil.data.models.SecurityQuestion
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.data.models.VisualModelOption
import com.haramveil.security.DeviceAdminController
import com.haramveil.security.SecurityQuestionsManager
import com.haramveil.service.HaramVeilProtectionController
import com.haramveil.ui.HaramVeilMainShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private enum class AppDestination(
    val route: String,
    val stepNumber: Int?,
) {
    Welcome(route = "welcome", stepNumber = 1),
    Capability(route = "capability", stepNumber = 2),
    AppSelection(route = "app_selection", stepNumber = 3),
    PinSetup(route = "pin_setup", stepNumber = 4),
    Permissions(route = "permissions", stepNumber = 5),
    AllSet(route = "all_set", stepNumber = 6),
    Dashboard(route = "dashboard", stepNumber = null),
}

@Composable
fun HaramVeilRoot(
    viewModel: OnboardingViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (uiState.isInitializing) {
        FullScreenLoading()
        return
    }

    LaunchedEffect(uiState.onboardingComplete) {
        if (uiState.onboardingComplete) {
            HaramVeilProtectionController.start(context)
        }
    }

    val navController = rememberNavController()
    val startDestination = if (uiState.onboardingComplete) {
        AppDestination.Dashboard.route
    } else {
        AppDestination.Welcome.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(durationMillis = 380),
                initialOffsetX = { it / 2 },
            ) + fadeIn(animationSpec = tween(380))
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 320),
                targetOffsetX = { -it / 4 },
            ) + fadeOut(animationSpec = tween(320))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(durationMillis = 320),
                initialOffsetX = { -it / 4 },
            ) + fadeIn(animationSpec = tween(320))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 320),
                targetOffsetX = { it / 2 },
            ) + fadeOut(animationSpec = tween(320))
        },
    ) {
        composable(AppDestination.Welcome.route) {
            WelcomeScreen(
                onBeginSetup = {
                    navController.navigate(AppDestination.Capability.route)
                },
            )
        }
        composable(AppDestination.Capability.route) {
            CapabilityBenchmarkScreen(
                uiState = uiState,
                onMode1Changed = viewModel::toggleMode1,
                onMode2Changed = viewModel::toggleMode2,
                onMode3Changed = viewModel::toggleMode3,
                onTextEngineSelected = viewModel::selectTextEngine,
                onVisualModelSelected = viewModel::selectVisualModel,
                onContinue = {
                    launchPersistAndNavigate(scope) {
                        viewModel.persistModeConfiguration()
                        navController.navigate(AppDestination.AppSelection.route)
                    }
                },
            )
        }
        composable(AppDestination.AppSelection.route) {
            AppSelectionScreen(
                uiState = uiState,
                onToggleApp = viewModel::toggleAppSelection,
                onSelectAll = viewModel::selectAllApps,
                onDeselectAll = viewModel::deselectAllApps,
                onContinue = {
                    launchPersistAndNavigate(scope) {
                        viewModel.persistSelectedApps()
                        navController.navigate(AppDestination.PinSetup.route)
                    }
                },
            )
        }
        composable(AppDestination.PinSetup.route) {
            PinSetupScreen(
                onContinue = { pin, securityAnswers ->
                    launchPersistAndNavigate(scope) {
                        viewModel.persistPinAndSecuritySetup(pin, securityAnswers)
                        navController.navigate(AppDestination.Permissions.route)
                    }
                },
            )
        }
        composable(AppDestination.Permissions.route) {
            PermissionsScreen(
                selectedTextEngine = uiState.selectedTextEngine,
                onContinue = {
                    navController.navigate(AppDestination.AllSet.route)
                },
            )
        }
        composable(AppDestination.AllSet.route) {
            AllSetScreen(
                uiState = uiState,
                onOpenDashboard = {
                    launchPersistAndNavigate(scope) {
                        viewModel.markOnboardingComplete()
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(AppDestination.Welcome.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable(AppDestination.Dashboard.route) {
            HaramVeilMainShell(
                selectedTextEngine = uiState.selectedTextEngine,
                selectedVisualModel = uiState.selectedVisualModel,
                supports640Model = uiState.benchmark.supportedModel == VisualModelOption.MODEL_640,
                mode1Enabled = uiState.mode1Enabled,
                mode2Enabled = uiState.mode2Enabled,
                mode3Enabled = uiState.mode3Enabled,
                installedApps = uiState.installedApps,
                selectedPackages = uiState.selectedPackages,
            )
        }
    }
}

@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text(
                text = "HaramVeil is loading...",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    onBeginSetup: () -> Unit,
) {
    OnboardingStepScreen(
        stepNumber = AppDestination.Welcome.stepNumber ?: 1,
        title = "Guard the eyes with calm, private protection",
        subtitle = "HaramVeil stays on-device and helps you lower the gaze before temptation turns into a habit.",
        footer = {
            Button(
                onClick = onBeginSetup,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Begin Setup")
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HaramVeilMotifLogo()
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                ),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "HaramVeil is built to help protect the heart by helping protect the eyes. It works quietly on your device, keeps your data local, and is designed around a simple Islamic goal: make the next good choice easier.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "\"Tell the believing men to lower their gaze and guard their chastity.\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Quran 24:30",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityBenchmarkScreen(
    uiState: OnboardingUiState,
    onMode1Changed: (Boolean) -> Unit,
    onMode2Changed: (Boolean) -> Unit,
    onMode3Changed: (Boolean) -> Unit,
    onTextEngineSelected: (TextRecognitionEngine) -> Unit,
    onVisualModelSelected: (VisualModelOption) -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingStepScreen(
        stepNumber = AppDestination.Capability.stepNumber ?: 2,
        title = "Benchmark your device and choose how HaramVeil reads text",
        subtitle = "Mode 3 is benchmarked silently in the background before you confirm your setup choices.",
        footer = {
            Button(
                onClick = onContinue,
                enabled = !uiState.benchmark.isRunning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Confirm Choices")
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (uiState.benchmark.isRunning) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Running quiet device benchmark",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Testing the 320 and 640 ONNX models so Mode 3 stays realistic for this device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                ModeCard(
                    title = "Mode 1 - Static node scanning",
                    description = "Reads visible app text, package names, and view IDs from the accessibility tree. This is the always-on, near-zero CPU guard.",
                    enabled = uiState.mode1Enabled,
                    onEnabledChanged = onMode1Changed,
                )
                ModeCard(
                    title = "Mode 2 - Text intelligence (OCR)",
                    description = "Only runs when Mode 1 raises a flag. HaramVeil crops key screen zones and checks them for risky text.",
                    enabled = uiState.mode2Enabled,
                    onEnabledChanged = onMode2Changed,
                )
                ModeCard(
                    title = "Mode 3 - Visual intelligence (ONNX)",
                    description = "Runs a slower image model in the background to catch explicit visual content. It depends on your benchmark result.",
                    enabled = uiState.mode3Enabled,
                    onEnabledChanged = onMode3Changed,
                    supportingContent = {
                        CapabilitySummaryCard(
                            uiState = uiState,
                            onVisualModelSelected = onVisualModelSelected,
                        )
                    },
                )

                SectionLabel(label = "Choose the text engine for Mode 2")
                EngineSelectionCard(
                    engine = TextRecognitionEngine.ML_KIT,
                    isSelected = uiState.selectedTextEngine == TextRecognitionEngine.ML_KIT,
                    isRecommended = uiState.playServicesAvailable,
                    title = "Google ML Kit",
                    description = "Usually faster, but it depends on Google Play Services being present on the device.",
                    privacyNote = "Good if your phone already uses Google services and you want the easiest path.",
                    onSelected = onTextEngineSelected,
                )
                EngineSelectionCard(
                    engine = TextRecognitionEngine.FOSS_ONNX,
                    isSelected = uiState.selectedTextEngine == TextRecognitionEngine.FOSS_ONNX,
                    isRecommended = uiState.isDeGoogledDevice,
                    title = "FOSS RapidOCR via ONNX",
                    description = "Fully open approach with on-device OCR models and no Play Services dependency.",
                    privacyNote = "Best for de-Googled or privacy-first setups. Model will download in the background on your approval.",
                    onSelected = onTextEngineSelected,
                )

                if (uiState.selectedTextEngine == TextRecognitionEngine.FOSS_ONNX) {
                    NoticeCard(
                        title = "FOSS OCR note",
                        body = "Model will download in background (~15MB). Internet permission will be removed after download.",
                        accentColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilitySummaryCard(
    uiState: OnboardingUiState,
    onVisualModelSelected: (VisualModelOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.benchmark.supportedModel != null) {
            NoticeCard(
                title = "Detected capability",
                body = "Your device can run the ${uiState.benchmark.supportedModel.displayName} model.",
                accentColor = MaterialTheme.colorScheme.secondary,
            )
        } else if (!uiState.benchmark.isRunning) {
            NoticeCard(
                title = "Detected capability",
                body = "Mode 3 did not clear the benchmark yet, so HaramVeil will keep visual intelligence disabled for now.",
                accentColor = MaterialTheme.colorScheme.error,
            )
        }

        if (uiState.benchmark.model320LatencyMs != null || uiState.benchmark.model640LatencyMs != null) {
            Text(
                text = buildString {
                    append("320 model: ")
                    append(uiState.benchmark.model320LatencyMs?.let { "$it ms" } ?: "failed")
                    append("  |  640 model: ")
                    append(uiState.benchmark.model640LatencyMs?.let { "$it ms" } ?: "failed")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.benchmark.supportedModel == VisualModelOption.MODEL_640) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.FilterChip(
                    selected = uiState.selectedVisualModel == VisualModelOption.MODEL_320,
                    onClick = { onVisualModelSelected(VisualModelOption.MODEL_320) },
                    label = { Text("Use 320") },
                )
                androidx.compose.material3.FilterChip(
                    selected = uiState.selectedVisualModel == VisualModelOption.MODEL_640,
                    onClick = { onVisualModelSelected(VisualModelOption.MODEL_640) },
                    label = { Text("Use 640") },
                )
            }
        }

        uiState.benchmark.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AppSelectionScreen(
    uiState: OnboardingUiState,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingBrush()),
    ) {
        DecorativeBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader(
                stepNumber = AppDestination.AppSelection.stepNumber ?: 3,
                title = "Choose which apps HaramVeil should watch",
                subtitle = "High-risk apps are pre-checked so setup starts from the safest default.",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onSelectAll,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Select All")
                }
                OutlinedButton(
                    onClick = onDeselectAll,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Deselect All")
                }
            }
            Text(
                text = "HIGH RISK apps are tagged in amber so you can review them quickly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ElevatedCard(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                ),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    items(
                        items = uiState.installedApps,
                        key = { app -> app.packageName },
                    ) { app ->
                        AppSelectionRow(
                            app = app,
                            selected = uiState.selectedPackages.contains(app.packageName),
                            onToggle = { onToggleApp(app.packageName) },
                        )
                    }
                }
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun PinSetupScreen(
    onContinue: (String, List<Pair<SecurityQuestion, Boolean>>) -> Unit,
) {
    val context = LocalContext.current
    val questionCatalog = remember(context) {
        SecurityQuestionsManager(context.applicationContext).questionCatalog()
    }
    var createdPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var pinStage by rememberSaveable { mutableStateOf("create") }
    var pinConfirmed by rememberSaveable { mutableStateOf(false) }
    var pinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedQuestionIds by rememberSaveable { mutableStateOf(listOf("", "", "")) }
    var selectedAnswers by rememberSaveable { mutableStateOf(listOf(-1, -1, -1)) }
    var activeDropdownSlot by rememberSaveable { mutableStateOf(-1) }

    val currentPinValue = if (pinStage == "create") createdPin else confirmPin
    val securityQuestionsComplete =
        pinConfirmed &&
            selectedQuestionIds.all { it.isNotBlank() } &&
            selectedQuestionIds.distinct().size == 3 &&
            selectedAnswers.all { it != -1 }

    OnboardingStepScreen(
        stepNumber = AppDestination.PinSetup.stepNumber ?: 4,
        title = "Lock your settings with a PIN",
        subtitle = "PIN uses bcrypt in encrypted storage. Security answers are saved as SHA-256 hashes only.",
        footer = {
            Button(
                onClick = {
                    val answers = selectedQuestionIds.zip(selectedAnswers).mapNotNull { (questionId, answerValue) ->
                        val question = questionCatalog.firstOrNull { it.id == questionId }
                        val answer = when (answerValue) {
                            1 -> true
                            0 -> false
                            else -> null
                        }
                        if (question != null && answer != null) {
                            question to answer
                        } else {
                            null
                        }
                    }
                    onContinue(createdPin, answers)
                },
                enabled = securityQuestionsComplete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = if (pinStage == "create") "Create a 6-digit PIN" else "Confirm the same 6-digit PIN",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    PinDots(pin = currentPinValue)
                    pinErrorMessage?.let { errorText ->
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    PinNumpad(
                        onDigitPressed = { digit ->
                            pinErrorMessage = null
                            if (pinStage == "create" && createdPin.length < 6) {
                                createdPin += digit
                            } else if (pinStage == "confirm" && confirmPin.length < 6) {
                                confirmPin += digit
                            }
                        },
                        onClear = {
                            pinErrorMessage = null
                            if (pinStage == "create") {
                                createdPin = ""
                            } else {
                                confirmPin = ""
                            }
                        },
                        onBackspace = {
                            pinErrorMessage = null
                            if (pinStage == "create" && createdPin.isNotEmpty()) {
                                createdPin = createdPin.dropLast(1)
                            } else if (pinStage == "confirm" && confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
                            }
                        },
                    )
                    if (pinStage == "create") {
                        Button(
                            onClick = {
                                pinStage = "confirm"
                                confirmPin = ""
                            },
                            enabled = createdPin.length == 6,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Confirm PIN")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (createdPin == confirmPin && createdPin.length == 6) {
                                    pinConfirmed = true
                                    pinErrorMessage = null
                                } else {
                                    pinConfirmed = false
                                    confirmPin = ""
                                    pinErrorMessage = "The two PIN entries did not match. Please try again."
                                }
                            },
                            enabled = confirmPin.length == 6,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Verify PIN")
                        }
                    }
                    TextButton(
                        onClick = {
                            createdPin = ""
                            confirmPin = ""
                            pinStage = "create"
                            pinConfirmed = false
                            pinErrorMessage = null
                        },
                    ) {
                        Text("Reset PIN")
                    }
                }
            }

            if (pinConfirmed) {
                SecurityQuestionSection(
                    questionCatalog = questionCatalog,
                    selectedQuestionIds = selectedQuestionIds,
                    selectedAnswers = selectedAnswers,
                    activeDropdownSlot = activeDropdownSlot,
                    onQuestionSelected = { slot, questionId ->
                        selectedQuestionIds = selectedQuestionIds.toMutableList().also { questions ->
                            questions[slot] = questionId
                        }
                    },
                    onAnswerSelected = { slot, answer ->
                        selectedAnswers = selectedAnswers.toMutableList().also { answers ->
                            answers[slot] = answer
                        }
                    },
                    onDropdownChanged = { slot ->
                        activeDropdownSlot = if (activeDropdownSlot == slot) -1 else slot
                    },
                )
            }
        }
    }
}

@Composable
private fun SecurityQuestionSection(
    questionCatalog: List<SecurityQuestion>,
    selectedQuestionIds: List<String>,
    selectedAnswers: List<Int>,
    activeDropdownSlot: Int,
    onQuestionSelected: (Int, String) -> Unit,
    onAnswerSelected: (Int, Int) -> Unit,
    onDropdownChanged: (Int) -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Pick 3 yes/no security questions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Choose questions you can answer consistently. The app never stores the plaintext answers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            repeat(3) { slot ->
                val selectedQuestionId = selectedQuestionIds[slot]
                val selectedQuestion = questionCatalog.firstOrNull { it.id == selectedQuestionId }
                val usedQuestionIds = selectedQuestionIds.filterIndexed { index, id ->
                    index != slot && id.isNotBlank()
                }.toSet()

                ExposedDropdownMenuBox(
                    expanded = activeDropdownSlot == slot,
                    onExpandedChange = { onDropdownChanged(slot) },
                ) {
                    OutlinedTextField(
                        value = selectedQuestion?.prompt ?: "Select question ${slot + 1}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Question ${slot + 1}") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = activeDropdownSlot == slot)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = activeDropdownSlot == slot,
                        onDismissRequest = { onDropdownChanged(slot) },
                    ) {
                        questionCatalog.forEach { question ->
                            DropdownMenuItem(
                                text = { Text(question.prompt) },
                                enabled = question.id !in usedQuestionIds,
                                onClick = {
                                    onQuestionSelected(slot, question.id)
                                    onDropdownChanged(slot)
                                },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedAnswers[slot] == 1,
                        onClick = { onAnswerSelected(slot, 1) },
                        label = { Text("Yes") },
                    )
                    FilterChip(
                        selected = selectedAnswers[slot] == 0,
                        onClick = { onAnswerSelected(slot, 0) },
                        label = { Text("No") },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionsScreen(
    selectedTextEngine: TextRecognitionEngine,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember(selectedTextEngine) {
        mutableStateOf(buildPermissionState(context, selectedTextEngine))
    }

    DisposableEffect(lifecycleOwner, selectedTextEngine) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = buildPermissionState(context, selectedTextEngine)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val missingPermissions = buildList {
        if (!permissionState.accessibilityGranted) add("Accessibility Service")
        if (!permissionState.overlayGranted) add("Draw Over Apps")
        if (!permissionState.deviceAdminGranted) add("Device Admin")
    }

    OnboardingStepScreen(
        stepNumber = AppDestination.Permissions.stepNumber ?: 5,
        title = "Grant the permissions HaramVeil needs",
        subtitle = "Each permission is explained in plain English so you can decide calmly. You can continue even if something stays off.",
        footer = {
            if (missingPermissions.isNotEmpty()) {
                Text(
                    text = "Without ${missingPermissions.joinToString()}, some protection features will stay limited until you enable them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (permissionState.allCorePermissionsGranted) "Next" else "Continue With Limited Protection")
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PermissionCard(
                title = "Accessibility Service",
                description = "Needed so HaramVeil can read the visible app UI tree and catch risky text before taking any screenshot.",
                isGranted = permissionState.accessibilityGranted,
                buttonLabel = "Open Accessibility Settings",
                onGrant = { openHaramVeilAccessibilitySettings(context) },
            )
            PermissionCard(
                title = "Draw Over Apps",
                description = "Needed to place the Veil overlay on top of risky content and guide you back to safety.",
                isGranted = permissionState.overlayGranted,
                buttonLabel = "Open Overlay Permission",
                onGrant = { openOverlaySettings(context) },
            )
            PermissionCard(
                title = "Device Admin",
                description = "Needed later for uninstall resistance and lockdown hardening, especially after a block event.",
                isGranted = permissionState.deviceAdminGranted,
                buttonLabel = "Open Device Admin",
                onGrant = { openDeviceAdminPrompt(context) },
            )
            PermissionCard(
                title = "Battery Optimization Exemption",
                description = "Needed so the system does not pause HaramVeil's background monitoring, especially on Samsung and Xiaomi devices.",
                isGranted = isBatteryOptimizationExempt(context),
                buttonLabel = "Request Exemption",
                onGrant = { requestBatteryOptimizationExemption(context) },
            )
            if (selectedTextEngine == TextRecognitionEngine.FOSS_ONNX) {
                PermissionCard(
                    title = "Private model storage",
                    description = "HaramVeil stores the OCR model inside app-private storage. No broad storage permission is requested, but you can review the app info page here.",
                    isGranted = permissionState.privateStorageReady,
                    buttonLabel = "Open App Info",
                    onGrant = { openAppInfo(context) },
                )
            }
        }
    }
}

@Composable
private fun AllSetScreen(
    uiState: OnboardingUiState,
    onOpenDashboard: () -> Unit,
) {
    OnboardingStepScreen(
        stepNumber = AppDestination.AllSet.stepNumber ?: 6,
        title = "Protection is now ACTIVE",
        subtitle = "Your onboarding choices are saved locally on device and HaramVeil will reopen to the dashboard next time.",
        footer = {
            Button(
                onClick = onOpenDashboard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Dashboard")
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NoticeCard(
                title = "Modes enabled",
                body = uiState.enabledModesSummary,
                accentColor = MaterialTheme.colorScheme.secondary,
            )
            NoticeCard(
                title = "Text engine selected",
                body = uiState.selectedTextEngine.displayName,
                accentColor = MaterialTheme.colorScheme.primary,
            )
            NoticeCard(
                title = "Apps being monitored",
                body = "${uiState.monitoredAppsCount} apps are selected for protection.",
                accentColor = MaterialTheme.colorScheme.tertiary,
            )
            uiState.selectedVisualModel?.let { visualModel ->
                NoticeCard(
                    title = "Visual model",
                    body = "Mode 3 is prepared with the ${visualModel.displayName} model.",
                    accentColor = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepScreen(
    stepNumber: Int,
    title: String,
    subtitle: String,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingBrush()),
    ) {
        DecorativeBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader(
                stepNumber = stepNumber,
                title = title,
                subtitle = subtitle,
            )
            content()
            Spacer(modifier = Modifier.height(8.dp))
            footer()
        }
    }
}

@Composable
private fun StepHeader(
    stepNumber: Int,
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Step $stepNumber of 6",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        LinearProgressIndicator(
            progress = { stepNumber / 6f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(100.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChanged,
                )
            }
            supportingContent()
        }
    }
}

@Composable
private fun EngineSelectionCard(
    engine: TextRecognitionEngine,
    isSelected: Boolean,
    isRecommended: Boolean,
    title: String,
    description: String,
    privacyNote: String,
    onSelected: (TextRecognitionEngine) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(engine) },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            },
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelected(engine) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (isRecommended) {
                    RecommendationBadge()
                }
            }
            Text(
                text = privacyNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun RecommendationBadge() {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = "Recommended",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun NoticeCard(
    title: String,
    body: String,
    accentColor: Color,
) {
    OutlinedCard(
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonLabel: String,
    onGrant: () -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                PermissionStatusPill(isGranted = isGranted)
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!isGranted) {
                Text(
                    text = "This feature will not work properly until you grant it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun PermissionStatusPill(isGranted: Boolean) {
    val backgroundColor = if (isGranted) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
    }
    val textColor = if (isGranted) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = if (isGranted) "Granted" else "Missing",
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun AppSelectionRow(
    app: InstalledAppInfo,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .clickable { onToggle() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
        )
        AppIcon(
            packageName = app.packageName,
            label = app.label,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (app.isHighRisk) {
                    RiskBadge()
                }
            }
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    label: String,
) {
    val context = LocalContext.current
    val iconDrawable by produceState<Drawable?>(initialValue = null, packageName) {
        value = runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }

    if (iconDrawable != null) {
        androidx.compose.foundation.Image(
            bitmap = iconDrawable!!.toBitmap(72, 72).asImageBitmap(),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RiskBadge() {
    Surface(
        color = Color(0xFFB7791F).copy(alpha = 0.18f),
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = "HIGH RISK",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFF6C065),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun PinDots(pin: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(6) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < pin.length) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PinNumpad(
    onDigitPressed: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        ).forEach { digits ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                digits.forEach { digit ->
                    NumpadButton(
                        label = digit,
                        modifier = Modifier.weight(1f),
                        onClick = { onDigitPressed(digit) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NumpadButton(
                label = "Clear",
                modifier = Modifier.weight(1f),
                onClick = onClear,
            )
            NumpadButton(
                label = "0",
                modifier = Modifier.weight(1f),
                onClick = { onDigitPressed("0") },
            )
            NumpadButton(
                label = "Back",
                modifier = Modifier.weight(1f),
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun NumpadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HaramVeilMotifLogo() {
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .rotate(45f)
                .border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(30.dp),
                ),
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(30.dp),
                ),
        )
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun DecorativeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.16f },
    ) {
        Box(
            modifier = Modifier
                .padding(start = 220.dp, top = 80.dp)
                .size(180.dp)
                .rotate(26f)
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(40.dp)),
        )
        Box(
            modifier = Modifier
                .padding(start = 12.dp, top = 420.dp)
                .size(140.dp)
                .rotate(18f)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f), RoundedCornerShape(28.dp)),
        )
    }
}

private fun onboardingBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(
            Color(0xFF08131F),
            Color(0xFF0D1B2A),
            Color(0xFF153425),
        ),
    )

private fun buildPermissionState(
    context: Context,
    selectedTextEngine: TextRecognitionEngine,
): PermissionReviewState {
    val accessibilityGranted = isHaramVeilAccessibilityServiceEnabled(context)
    val overlayGranted = Settings.canDrawOverlays(context)
    val deviceAdminGranted = isDeviceAdminEnabled(context)
    return PermissionReviewState(
        accessibilityGranted = accessibilityGranted,
        overlayGranted = overlayGranted,
        deviceAdminGranted = deviceAdminGranted,
        privateStorageReady = true,
    )
}

private fun isDeviceAdminEnabled(context: Context): Boolean {
    return DeviceAdminController.isEnabled(context)
}

private fun openOverlaySettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

private fun openDeviceAdminPrompt(context: Context) {
    DeviceAdminController.openEnablePrompt(
        context = context,
        explanation = "HaramVeil uses Device Admin for uninstall resistance and stronger tamper protection after a block.",
    )
}

private fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

private fun launchPersistAndNavigate(
    scope: CoroutineScope,
    action: suspend () -> Unit,
) {
    scope.launch {
        action()
    }
}

private fun isBatteryOptimizationExempt(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestBatteryOptimizationExemption(context: Context) {
    if (isBatteryOptimizationExempt(context)) return
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    } catch (_: Exception) {
        // Fallback: open the general battery optimization list.
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

