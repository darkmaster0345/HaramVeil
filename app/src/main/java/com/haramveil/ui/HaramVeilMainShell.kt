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

package com.haramveil.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haramveil.accessibility.isHaramVeilAccessibilityServiceEnabled
import com.haramveil.accessibility.openHaramVeilAccessibilitySettings
import com.haramveil.data.local.DefaultKeywordBlocklist
import com.haramveil.data.local.ProtectionPreferencesRepository
import com.haramveil.data.models.InstalledAppInfo
import com.haramveil.data.models.ProtectionSettings
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.data.models.VisualModelOption
import com.haramveil.detection.mode3.ModelSelectionState
import com.haramveil.detection.mode3.ModelSelector
import com.haramveil.security.AppLockdownManager
import com.haramveil.security.DeviceAdminController
import com.haramveil.security.PinManager
import com.haramveil.security.SecurityQuestionsManager
import com.haramveil.service.HaramVeilProtectionController
import com.haramveil.service.ProtectionBootstrapStore
import com.haramveil.service.TamperEventSnapshot
import com.haramveil.ui.dashboard.DashboardRoute
import com.haramveil.ui.dashboard.DashboardScreen
import com.haramveil.ui.settings.AdvancedSettingsScreen
import com.haramveil.ui.settings.SettingsRoute
import com.haramveil.ui.settings.SettingsScreen
import com.haramveil.ui.security.ChangePINScreen
import com.haramveil.ui.security.PinActionGateDialog
import com.haramveil.ui.security.PinGateComposable
import com.haramveil.ui.stats.StatsRoute
import com.haramveil.ui.stats.StatsScreen
import com.haramveil.utils.BatteryOptimizationHelper
import com.haramveil.utils.RootAccessHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private enum class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Dashboard(
        route = DashboardRoute.route,
        label = "Dashboard",
        icon = Icons.Outlined.Home,
    ),
    Stats(
        route = StatsRoute.route,
        label = "Stats",
        icon = Icons.Outlined.BarChart,
    ),
    Settings(
        route = SettingsRoute.route,
        label = "Settings",
        icon = Icons.Outlined.Settings,
    ),
}

private enum class SensitiveAction {
    DISABLE_PROTECTION,
    CLEAR_HISTORY,
}

@Composable
fun HaramVeilMainShell(
    selectedTextEngine: TextRecognitionEngine,
    selectedVisualModel: VisualModelOption?,
    supports640Model: Boolean,
    mode1Enabled: Boolean,
    mode2Enabled: Boolean,
    mode3Enabled: Boolean,
    installedApps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val protectionPreferencesRepository = remember(context) {
        ProtectionPreferencesRepository(context.applicationContext)
    }
    val appLockdownManager = remember(context) {
        AppLockdownManager(context.applicationContext)
    }
    val bootstrapStore = remember(context) {
        ProtectionBootstrapStore(context.applicationContext)
    }
    val pinManager = remember(context) {
        PinManager(context.applicationContext)
    }
    val securityQuestionsManager = remember(context) {
        SecurityQuestionsManager(context.applicationContext)
    }
    val modelSelector = remember(context, protectionPreferencesRepository) {
        ModelSelector(
            context = context.applicationContext,
            protectionPreferencesRepository = protectionPreferencesRepository,
        )
    }
    val batteryOptimizationGuide = remember(context) {
        BatteryOptimizationHelper.resolveGuide(context.applicationContext)
    }
    val rootModeEnabled = remember(context) {
        RootAccessHelper.isRootAvailable()
    }
    val initialProtectionSettings = remember(
        selectedTextEngine,
        selectedVisualModel,
        mode1Enabled,
        mode2Enabled,
        mode3Enabled,
        selectedPackages,
    ) {
        ProtectionSettings(
            protectionEnabled = true,
            monitoredPackages = selectedPackages,
            keywordBlocklist = DefaultKeywordBlocklist.entries,
            mode1Enabled = mode1Enabled,
            mode2Enabled = mode2Enabled,
            mode3Enabled = mode3Enabled,
            selectedTextEngine = selectedTextEngine,
            selectedVisualModel = selectedVisualModel,
        )
    }
    val protectionSettings by protectionPreferencesRepository.settingsFlow.collectAsStateWithLifecycle(
        initialValue = initialProtectionSettings,
    )
    var resolvedModelSelectionState by remember { mutableStateOf<ModelSelectionState?>(null) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val accessibilityServiceActive = rememberAccessibilityServiceStatus(
        lifecycleOwner = lifecycleOwner,
        context = context,
    )
    val monitoredPackages = protectionSettings.monitoredPackages.ifEmpty { selectedPackages }
    val initialAppConfigs = remember(installedApps, monitoredPackages) {
        buildInitialAppConfigs(
            installedApps = installedApps,
            selectedPackages = monitoredPackages,
        )
    }
    var appConfigs by remember(initialAppConfigs) { mutableStateOf(initialAppConfigs) }
    var settingsUnlocked by rememberSaveable { mutableStateOf(false) }
    var pendingSensitiveAction by remember { mutableStateOf<SensitiveAction?>(null) }
    var foregroundServiceActive by remember { mutableStateOf(bootstrapStore.isServiceAlive()) }
    var deviceAdminEnabled by remember { mutableStateOf(DeviceAdminController.isEnabled(context.applicationContext)) }
    var latestTamperEvent by remember { mutableStateOf(bootstrapStore.lastTamperEvent()) }
    var showBatteryOptimizationDialog by rememberSaveable { mutableStateOf(false) }
    val activeTextEngine = protectionSettings.selectedTextEngine
    val activeVisualModel = resolvedModelSelectionState?.modelConfig?.visualModel ?: protectionSettings.selectedVisualModel ?: when {
        selectedVisualModel != null -> selectedVisualModel
        supports640Model -> VisualModelOption.MODEL_640
        else -> VisualModelOption.MODEL_320
    }
    val canOffer640Model = resolvedModelSelectionState?.supports640Model ?: supports640Model
    val globalLockdownDuration = remember(protectionSettings.lockdownDurationMs) {
        LockdownDurationOption.fromDurationMs(protectionSettings.lockdownDurationMs)
    }
    val keywordBlocklist = protectionSettings.keywordBlocklist
    val mode1OverrideEnabled = protectionSettings.mode1Enabled
    val mode2OverrideEnabled = protectionSettings.mode2Enabled
    val mode3OverrideEnabled = protectionSettings.mode3Enabled
    val frameSkipIntervalMs = protectionSettings.frameSkipIntervalMs.toFloat()
    val mode3InferenceIntervalMs = protectionSettings.mode3InferenceIntervalMs.toFloat()
    val topCapturePercent = protectionSettings.topCapturePercent.toFloat()
    val middleCapturePercent = protectionSettings.middleCapturePercent.toFloat()
    var baseBlockEvents by remember(initialAppConfigs) {
        mutableStateOf(
            buildSampleBlockEvents(
                monitoredApps = initialAppConfigs.filter { it.isMonitored }.map { it.app },
            ),
        )
    }
    val blockEvents = remember(baseBlockEvents, latestTamperEvent) {
        mergeBlockEvents(
            contentEvents = baseBlockEvents,
            tamperEvent = latestTamperEvent,
        )
    }
    var activeLockdowns by remember { mutableStateOf(emptyList<ActiveLockdownUiModel>()) }

    val monitoredApps = appConfigs.filter { it.isMonitored }
    val knownAppsByPackage = remember(appConfigs, installedApps) {
        buildMap {
            installedApps.forEach { app ->
                put(app.packageName, app)
            }
            appConfigs.forEach { appConfig ->
                put(appConfig.app.packageName, appConfig.app)
            }
        }
    }
    val activeModeCount = listOf(
        mode1OverrideEnabled,
        mode2OverrideEnabled,
        mode3OverrideEnabled,
    ).count { it }
    val activeModeSummary = buildActiveModeSummary(
        mode1Enabled = mode1OverrideEnabled,
        mode2Enabled = mode2OverrideEnabled,
        mode3Enabled = mode3OverrideEnabled,
    )
    val blockCounts = remember(blockEvents) { deriveBlockCounts(blockEvents) }
    val mostBlockedApp = remember(blockEvents) { deriveMostBlockedApp(blockEvents) }
    val activeLockdownMessage = remember(activeLockdowns) {
        activeLockdowns.firstOrNull()?.let { lockdown ->
            "${lockdown.app.label} locked for ${lockdown.remainingLabel} remaining"
        }
    }
    val selectedBottomDestination = when (currentRoute) {
        SettingsRoute.advancedRoute,
        SettingsRoute.changePinRoute,
        -> MainDestination.Settings.route
        else -> currentRoute
    }

    LaunchedEffect(protectionSettings.protectionEnabled) {
        if (protectionSettings.protectionEnabled) {
            HaramVeilProtectionController.start(context.applicationContext)
        } else {
            HaramVeilProtectionController.stop(context.applicationContext)
        }
    }

    LaunchedEffect(
        accessibilityServiceActive,
        protectionSettings.accessibilitySettingsPromptShown,
    ) {
        if (!accessibilityServiceActive && !protectionSettings.accessibilitySettingsPromptShown) {
            openHaramVeilAccessibilitySettings(context)
            protectionPreferencesRepository.markAccessibilitySettingsPromptShown()
        }
    }

    LaunchedEffect(
        protectionSettings.selectedVisualModel,
    ) {
        resolvedModelSelectionState = modelSelector.readSelectionState()
    }

    LaunchedEffect(
        protectionSettings.batteryOptimizationCompleted,
        protectionSettings.batteryOptimizationPromptShown,
    ) {
        if (!protectionSettings.batteryOptimizationCompleted &&
            !protectionSettings.batteryOptimizationPromptShown
        ) {
            showBatteryOptimizationDialog = true
            protectionPreferencesRepository.markBatteryOptimizationPromptShown()
        }
    }

    LaunchedEffect(knownAppsByPackage) {
        while (true) {
            activeLockdowns = appLockdownManager.activeLockdowns().map { lockdown ->
                val app = knownAppsByPackage[lockdown.packageName] ?: fallbackInstalledApp(lockdown.packageName)
                ActiveLockdownUiModel(
                    app = app,
                    remainingDurationMs = lockdown.remainingDurationMs(),
                    remainingLabel = formatRemainingDuration(lockdown.remainingDurationMs()),
                )
            }
            foregroundServiceActive = bootstrapStore.isServiceAlive()
            deviceAdminEnabled = DeviceAdminController.isEnabled(context.applicationContext)
            latestTamperEvent = bootstrapStore.lastTamperEvent()
            delay(1_000L)
        }
    }

    HaramVeilScreenBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                ) {
                    MainDestination.entries.forEach { destination ->
                        val isSelected = navBackStackEntry?.destination
                            ?.hierarchy
                            ?.any { it.route == destination.route } == true ||
                            selectedBottomDestination == destination.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute.route,
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 360),
                            initialOffsetX = { it / 3 },
                        ) + fadeIn(animationSpec = tween(360))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 280),
                            targetOffsetX = { -it / 5 },
                        ) + fadeOut(animationSpec = tween(280))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 280),
                            initialOffsetX = { -it / 5 },
                        ) + fadeIn(animationSpec = tween(280))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 280),
                            targetOffsetX = { it / 3 },
                        ) + fadeOut(animationSpec = tween(280))
                    },
                ) {
                    composable(DashboardRoute.route) {
                        DashboardScreen(
                            protectionEnabled = protectionSettings.protectionEnabled,
                            activeModeSummary = activeModeSummary,
                            activeModeCount = activeModeCount,
                            monitoredAppsCount = monitoredApps.size,
                            blocksToday = blockCounts.today,
                            accessibilityServiceActive = accessibilityServiceActive,
                            deviceAdminEnabled = deviceAdminEnabled,
                            foregroundServiceActive = foregroundServiceActive,
                            rootModeEnabled = rootModeEnabled,
                            activeLockdowns = activeLockdowns,
                            batteryOptimizationCompleted = protectionSettings.batteryOptimizationCompleted,
                            batteryOptimizationManufacturerLabel = batteryOptimizationGuide.manufacturerLabel,
                            recentEvents = blockEvents.take(3),
                            onProtectionEnabledChange = { shouldEnable ->
                                scope.launch {
                                    protectionPreferencesRepository.saveProtectionEnabled(shouldEnable)
                                }
                            },
                            onRequestProtectionDisable = {
                                pendingSensitiveAction = SensitiveAction.DISABLE_PROTECTION
                            },
                            onOpenAccessibilitySettings = {
                                openHaramVeilAccessibilitySettings(context)
                            },
                            onOpenBatteryOptimizationGuide = {
                                showBatteryOptimizationDialog = true
                            },
                            onMarkBatteryOptimizationCompleted = {
                                scope.launch {
                                    protectionPreferencesRepository.markBatteryOptimizationCompleted()
                                }
                            },
                            onViewFullStats = {
                                navController.navigate(StatsRoute.route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable(StatsRoute.route) {
                        StatsScreen(
                            events = blockEvents,
                            todayCount = blockCounts.today,
                            thisWeekCount = blockCounts.thisWeek,
                            allTimeCount = blockCounts.allTime,
                            mostBlockedApp = mostBlockedApp,
                            onRequestClearHistory = {
                                pendingSensitiveAction = SensitiveAction.CLEAR_HISTORY
                            },
                        )
                    }
                    composable(SettingsRoute.route) {
                        PinGateComposable(
                            authenticated = settingsUnlocked,
                            pinManager = pinManager,
                            securityQuestionsManager = securityQuestionsManager,
                            title = "Enter your PIN to open Settings",
                            subtitle = "HaramVeil protects its settings behind the same local PIN that guards disabling protection and clearing history.",
                            onAuthenticated = {
                                settingsUnlocked = true
                            },
                        ) {
                            SettingsScreen(
                                monitoredApps = appConfigs,
                                selectedEngine = activeTextEngine,
                                globalLockdownDuration = globalLockdownDuration,
                                keywordBlocklist = keywordBlocklist,
                                deviceAdminEnabled = deviceAdminEnabled,
                                onOpenAdvancedSettings = {
                                    navController.navigate(SettingsRoute.advancedRoute)
                                },
                                onOpenChangePin = {
                                    navController.navigate(SettingsRoute.changePinRoute)
                                },
                                onEngineSelected = { engine ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveModeConfiguration(
                                            textEngine = engine,
                                            visualModel = activeVisualModel,
                                            mode1Enabled = mode1OverrideEnabled,
                                            mode2Enabled = mode2OverrideEnabled,
                                            mode3Enabled = mode3OverrideEnabled,
                                        )
                                    }
                                },
                                onMonitoredAppsUpdated = { selectedPackageNames ->
                                    appConfigs = appConfigs.map { config ->
                                        config.copy(isMonitored = config.app.packageName in selectedPackageNames)
                                    }
                                    scope.launch {
                                        protectionPreferencesRepository.saveMonitoredPackages(selectedPackageNames)
                                    }
                                },
                                onKeywordAdded = { keyword ->
                                    if (keyword.isNotBlank() && keyword !in keywordBlocklist) {
                                        scope.launch {
                                            protectionPreferencesRepository.saveKeywordBlocklist(
                                                keywordBlocklist + keyword,
                                            )
                                        }
                                    }
                                },
                                onKeywordRemoved = { keyword ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveKeywordBlocklist(
                                            keywordBlocklist - keyword,
                                        )
                                    }
                                },
                                onLockdownDurationSelected = { duration ->
                                    scope.launch {
                                        val persistedDurationMs = when (duration) {
                                            LockdownDurationOption.CUSTOM -> LockdownDurationOption.DefaultCustomDurationMs
                                            else -> duration.toDurationMs(protectionSettings.lockdownDurationMs)
                                        }
                                        protectionPreferencesRepository.saveLockdownDurationMs(persistedDurationMs)
                                    }
                                },
                            )
                        }
                    }
                    composable(SettingsRoute.advancedRoute) {
                        PinGateComposable(
                            authenticated = settingsUnlocked,
                            pinManager = pinManager,
                            securityQuestionsManager = securityQuestionsManager,
                            title = "Enter your PIN to open Advanced Settings",
                            subtitle = "Advanced controls can weaken or strengthen protection, so HaramVeil keeps them behind the same local PIN gate.",
                            onAuthenticated = {
                                settingsUnlocked = true
                            },
                        ) {
                            AdvancedSettingsScreen(
                                appConfigs = monitoredApps,
                                supports640Model = canOffer640Model,
                                selectedVisualModel = activeVisualModel,
                                mode1Enabled = mode1OverrideEnabled,
                                mode2Enabled = mode2OverrideEnabled,
                                mode3Enabled = mode3OverrideEnabled,
                                frameSkipIntervalMs = frameSkipIntervalMs.toInt(),
                                mode3InferenceIntervalMs = mode3InferenceIntervalMs.toInt(),
                                topCapturePercent = topCapturePercent.toInt(),
                                middleCapturePercent = middleCapturePercent.toInt(),
                                onBack = { navController.popBackStack() },
                                onMode1Changed = { enabled ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveModeConfiguration(
                                            textEngine = activeTextEngine,
                                            visualModel = activeVisualModel,
                                            mode1Enabled = enabled,
                                            mode2Enabled = mode2OverrideEnabled,
                                            mode3Enabled = mode3OverrideEnabled,
                                        )
                                    }
                                },
                                onMode2Changed = { enabled ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveModeConfiguration(
                                            textEngine = activeTextEngine,
                                            visualModel = activeVisualModel,
                                            mode1Enabled = mode1OverrideEnabled,
                                            mode2Enabled = enabled,
                                            mode3Enabled = mode3OverrideEnabled,
                                        )
                                    }
                                },
                                onMode3Changed = { enabled ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveModeConfiguration(
                                            textEngine = activeTextEngine,
                                            visualModel = activeVisualModel,
                                            mode1Enabled = mode1OverrideEnabled,
                                            mode2Enabled = mode2OverrideEnabled,
                                            mode3Enabled = enabled,
                                        )
                                    }
                                },
                                onVisualModelSelected = { model ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveModeConfiguration(
                                            textEngine = activeTextEngine,
                                            visualModel = model,
                                            mode1Enabled = mode1OverrideEnabled,
                                            mode2Enabled = mode2OverrideEnabled,
                                            mode3Enabled = mode3OverrideEnabled,
                                        )
                                    }
                                },
                                onFrameSkipIntervalChanged = { intervalMs ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveFrameSkipIntervalMs(intervalMs.toLong())
                                    }
                                },
                                onMode3InferenceIntervalChanged = { intervalMs ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveMode3InferenceIntervalMs(intervalMs.toLong())
                                    }
                                },
                                onTopCapturePercentChanged = { percent ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveHaramClipConfiguration(
                                            topCapturePercent = percent,
                                            middleCapturePercent = protectionSettings.middleCapturePercent,
                                        )
                                    }
                                },
                                onMiddleCapturePercentChanged = { percent ->
                                    scope.launch {
                                        protectionPreferencesRepository.saveHaramClipConfiguration(
                                            topCapturePercent = protectionSettings.topCapturePercent,
                                            middleCapturePercent = percent,
                                        )
                                    }
                                },
                                onModeOverrideChanged = { packageName, option ->
                                    appConfigs = appConfigs.map { config ->
                                        if (config.app.packageName == packageName) {
                                            config.copy(modeOverride = option)
                                        } else {
                                            config
                                        }
                                    }
                                },
                                onLockdownOverrideChanged = { packageName, option ->
                                    appConfigs = appConfigs.map { config ->
                                        if (config.app.packageName == packageName) {
                                            config.copy(lockdownOverride = option)
                                        } else {
                                            config
                                        }
                                    }
                                },
                            )
                        }
                    }
                    composable(SettingsRoute.changePinRoute) {
                        PinGateComposable(
                            authenticated = settingsUnlocked,
                            pinManager = pinManager,
                            securityQuestionsManager = securityQuestionsManager,
                            title = "Enter your PIN to manage security",
                            subtitle = "Changing the PIN starts from a protected settings route, then asks you to verify the current PIN again before saving a new one.",
                            onAuthenticated = {
                                settingsUnlocked = true
                            },
                        ) {
                            ChangePINScreen(
                                pinManager = pinManager,
                                securityQuestionsManager = securityQuestionsManager,
                                onBack = { navController.popBackStack() },
                                onPinChanged = {
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = activeLockdownMessage != null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 12.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lockdown active",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = activeLockdownMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                PinActionGateDialog(
                    visible = pendingSensitiveAction != null,
                    pinManager = pinManager,
                    securityQuestionsManager = securityQuestionsManager,
                    title = when (pendingSensitiveAction) {
                        SensitiveAction.DISABLE_PROTECTION -> "Enter your PIN to turn protection off"
                        SensitiveAction.CLEAR_HISTORY -> "Enter your PIN to clear history"
                        null -> ""
                    },
                    subtitle = when (pendingSensitiveAction) {
                        SensitiveAction.DISABLE_PROTECTION -> "HaramVeil asks for the local PIN before protection can be paused, even for a moment."
                        SensitiveAction.CLEAR_HISTORY -> "Clearing local block history is protected so it cannot be wiped casually or impulsively."
                        null -> ""
                    },
                    onDismiss = {
                        pendingSensitiveAction = null
                    },
                    onAuthenticated = {
                        when (pendingSensitiveAction) {
                            SensitiveAction.DISABLE_PROTECTION -> {
                                scope.launch {
                                    protectionPreferencesRepository.saveProtectionEnabled(false)
                                }
                            }
                            SensitiveAction.CLEAR_HISTORY -> {
                                baseBlockEvents = emptyList()
                                bootstrapStore.clearTamperEvent()
                                latestTamperEvent = null
                            }
                            null -> Unit
                        }
                        pendingSensitiveAction = null
                    },
                )

                if (showBatteryOptimizationDialog) {
                    AlertDialog(
                        onDismissRequest = { showBatteryOptimizationDialog = false },
                        title = { Text(batteryOptimizationGuide.title) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = batteryOptimizationGuide.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                batteryOptimizationGuide.steps.forEachIndexed { index, step ->
                                    Text(
                                        text = "${index + 1}. $step",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showBatteryOptimizationDialog = false
                                    BatteryOptimizationHelper.openGuide(
                                        context = context.applicationContext,
                                        guide = batteryOptimizationGuide,
                                    )
                                },
                            ) {
                                Text(batteryOptimizationGuide.buttonLabel)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showBatteryOptimizationDialog = false
                                    scope.launch {
                                        protectionPreferencesRepository.markBatteryOptimizationCompleted()
                                    }
                                },
                            ) {
                                Text("I've done this")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberAccessibilityServiceStatus(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: android.content.Context,
): Boolean {
    var isActive by remember {
        mutableStateOf(isHaramVeilAccessibilityServiceEnabled(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isActive = isHaramVeilAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return isActive
}

private data class BlockCountSummary(
    val today: Int,
    val thisWeek: Int,
    val allTime: Int,
)

private fun buildInitialAppConfigs(
    installedApps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
): List<AppMonitoringUiModel> {
    val fallbackApps = listOf(
        InstalledAppInfo(
            packageName = "com.instagram.android",
            label = "Instagram",
            isHighRisk = true,
        ),
        InstalledAppInfo(
            packageName = "com.android.chrome",
            label = "Chrome",
            isHighRisk = true,
        ),
        InstalledAppInfo(
            packageName = "com.google.android.youtube",
            label = "YouTube",
            isHighRisk = true,
        ),
        InstalledAppInfo(
            packageName = "org.mozilla.firefox",
            label = "Firefox",
            isHighRisk = true,
        ),
        InstalledAppInfo(
            packageName = "org.telegram.messenger",
            label = "Telegram",
            isHighRisk = true,
        ),
    )
    val appSource = if (installedApps.isEmpty()) fallbackApps else installedApps
    val selectedSet = if (selectedPackages.isEmpty()) {
        appSource.filter { it.isHighRisk }.take(5).map { it.packageName }.toSet()
    } else {
        selectedPackages
    }
    return appSource
        .sortedWith(compareByDescending<InstalledAppInfo> { it.isHighRisk }.thenBy { it.label.lowercase() })
        .mapIndexed { index, app ->
            AppMonitoringUiModel(
                app = app,
                isMonitored = app.packageName in selectedSet,
                modeOverride = when {
                    index == 0 && app.packageName in selectedSet -> ModeOverrideOption.FORCE_MODE_3
                    index == 1 && app.packageName in selectedSet -> ModeOverrideOption.FORCE_MODE_2
                    else -> ModeOverrideOption.DEFAULT
                },
                lockdownOverride = when {
                    index == 0 && app.packageName in selectedSet -> LockdownDurationOption.MINUTES_30
                    index == 1 && app.packageName in selectedSet -> LockdownDurationOption.HOUR_1
                    else -> LockdownDurationOption.DEFAULT
                },
            )
        }
}

private fun buildSampleBlockEvents(
    monitoredApps: List<InstalledAppInfo>,
): List<BlockEventUiModel> {
    val appPool = if (monitoredApps.isEmpty()) {
        listOf(
            InstalledAppInfo("com.instagram.android", "Instagram", true),
            InstalledAppInfo("com.android.chrome", "Chrome", true),
            InstalledAppInfo("com.google.android.youtube", "YouTube", true),
        )
    } else {
        monitoredApps.take(4)
    }
    val safePool = generateSequence { appPool }.flatten().take(4).toList()
    val now = LocalDateTime.now()
    return listOf(
        BlockEventUiModel(
            id = "event_1",
            app = safePool[0],
            mode = BlockDetectionMode.MODE_2,
            timestamp = now.minusMinutes(11),
        ),
        BlockEventUiModel(
            id = "event_2",
            app = safePool[1],
            mode = BlockDetectionMode.MODE_1,
            timestamp = now.minusMinutes(42),
        ),
        BlockEventUiModel(
            id = "event_3",
            app = safePool[2],
            mode = BlockDetectionMode.MODE_3,
            timestamp = now.minusHours(2),
        ),
        BlockEventUiModel(
            id = "event_4",
            app = safePool[0],
            mode = BlockDetectionMode.MODE_2,
            timestamp = now.minusDays(1).minusMinutes(28),
        ),
        BlockEventUiModel(
            id = "event_5",
            app = safePool[1],
            mode = BlockDetectionMode.MODE_1,
            timestamp = now.minusDays(3).minusHours(4),
        ),
        BlockEventUiModel(
            id = "event_6",
            app = safePool[3],
            mode = BlockDetectionMode.MODE_3,
            timestamp = now.minusDays(6).minusHours(1),
        ),
    ).sortedByDescending { it.timestamp }
}

private fun buildActiveModeSummary(
    mode1Enabled: Boolean,
    mode2Enabled: Boolean,
    mode3Enabled: Boolean,
): String {
    val labels = buildList {
        if (mode1Enabled) add("Mode 1")
        if (mode2Enabled) add("Mode 2")
        if (mode3Enabled) add("Mode 3")
    }
    return when {
        labels.isEmpty() -> "Paused"
        labels.size == 3 -> "All three modes"
        labels.size == 1 -> labels.first()
        else -> labels.joinToString(separator = " + ")
    }
}

private fun deriveBlockCounts(
    events: List<BlockEventUiModel>,
): BlockCountSummary {
    val contentEvents = events.filter { event -> event.mode != BlockDetectionMode.SYSTEM_ALERT }
    val today = LocalDate.now()
    val weekStart = today.minusDays(6)
    return BlockCountSummary(
        today = contentEvents.count { it.timestamp.toLocalDate() == today },
        thisWeek = contentEvents.count { !it.timestamp.toLocalDate().isBefore(weekStart) },
        allTime = contentEvents.size,
    )
}

private fun deriveMostBlockedApp(
    events: List<BlockEventUiModel>,
): MostBlockedAppUiModel? {
    return events
        .filter { event -> event.mode != BlockDetectionMode.SYSTEM_ALERT }
        .groupingBy { it.app.packageName }
        .eachCount()
        .maxByOrNull { it.value }
        ?.let { entry ->
            val representativeApp = events.first { it.app.packageName == entry.key }.app
            MostBlockedAppUiModel(
                app = representativeApp,
                count = entry.value,
            )
        }
}

private fun fallbackInstalledApp(
    packageName: String,
): InstalledAppInfo {
    val fallbackLabel = packageName.substringAfterLast('.')
        .replace('_', ' ')
        .replace('-', ' ')
        .replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase() else character.toString()
        }
    return InstalledAppInfo(
        packageName = packageName,
        label = fallbackLabel,
        isHighRisk = false,
    )
}

private fun mergeBlockEvents(
    contentEvents: List<BlockEventUiModel>,
    tamperEvent: TamperEventSnapshot?,
): List<BlockEventUiModel> {
    val tamperEventModel = tamperEvent?.let { snapshot ->
        BlockEventUiModel(
            id = "tamper_${snapshot.eventType}_${snapshot.timestampMs}",
            app = InstalledAppInfo(
                packageName = "com.haramveil.system",
                label = "Tamper Protection",
                isHighRisk = false,
            ),
            mode = BlockDetectionMode.SYSTEM_ALERT,
            timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(snapshot.timestampMs),
                ZoneId.systemDefault(),
            ),
        )
    }
    return buildList {
        tamperEventModel?.let(::add)
        addAll(contentEvents)
    }.sortedByDescending { event -> event.timestamp }
}

private fun formatRemainingDuration(
    remainingDurationMs: Long,
): String {
    val totalSeconds = (remainingDurationMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return when {
        minutes > 0L -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
        else -> "${seconds}s"
    }
}
