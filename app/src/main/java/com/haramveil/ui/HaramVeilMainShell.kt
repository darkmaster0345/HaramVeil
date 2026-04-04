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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haramveil.data.models.InstalledAppInfo
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.data.models.VisualModelOption
import com.haramveil.ui.dashboard.DashboardRoute
import com.haramveil.ui.dashboard.DashboardScreen
import com.haramveil.ui.settings.AdvancedSettingsScreen
import com.haramveil.ui.settings.SettingsRoute
import com.haramveil.ui.settings.SettingsScreen
import com.haramveil.ui.stats.StatsRoute
import com.haramveil.ui.stats.StatsScreen
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val initialAppConfigs = remember(installedApps, selectedPackages) {
        buildInitialAppConfigs(
            installedApps = installedApps,
            selectedPackages = selectedPackages,
        )
    }
    var appConfigs by remember(initialAppConfigs) { mutableStateOf(initialAppConfigs) }
    var protectionEnabled by remember { mutableStateOf(true) }
    var activeTextEngine by remember(selectedTextEngine) { mutableStateOf(selectedTextEngine) }
    var activeVisualModel by remember(selectedVisualModel, supports640Model) {
        mutableStateOf(
            when {
                selectedVisualModel != null -> selectedVisualModel
                supports640Model -> VisualModelOption.MODEL_640
                else -> VisualModelOption.MODEL_320
            },
        )
    }
    var globalLockdownDuration by remember { mutableStateOf(LockdownDurationOption.MINUTES_15) }
    var keywordBlocklist by remember {
        mutableStateOf(
            listOf(
                "nsfw",
                "dating",
                "OnlyFans",
                "(?i)escort",
                "(?i)cam\\s?girl",
            ),
        )
    }
    var mode1OverrideEnabled by remember(mode1Enabled) { mutableStateOf(mode1Enabled) }
    var mode2OverrideEnabled by remember(mode2Enabled) { mutableStateOf(mode2Enabled) }
    var mode3OverrideEnabled by remember(mode3Enabled) { mutableStateOf(mode3Enabled) }
    var frameSkipIntervalMs by remember { mutableFloatStateOf(500f) }
    var topCapturePercent by remember { mutableFloatStateOf(30f) }
    var middleCapturePercent by remember { mutableFloatStateOf(40f) }
    var blockEvents by remember(initialAppConfigs) {
        mutableStateOf(
            buildSampleBlockEvents(
                monitoredApps = initialAppConfigs.filter { it.isMonitored }.map { it.app },
            ),
        )
    }

    val monitoredApps = appConfigs.filter { it.isMonitored }
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
    val activeLockdownMessage = remember(blockEvents, protectionEnabled, globalLockdownDuration) {
        blockEvents.firstOrNull()?.takeIf { protectionEnabled }?.let { latestEvent ->
            val targetMinutes = globalLockdownDuration.minutes ?: 45
            val elapsedMinutes = Duration.between(latestEvent.timestamp, LocalDateTime.now()).toMinutes()
            val remainingMinutes = (targetMinutes - elapsedMinutes).coerceAtLeast(1)
            "${latestEvent.app.label} locked for $remainingMinutes minute${if (remainingMinutes == 1L) "" else "s"} remaining"
        }
    }
    val selectedBottomDestination = when (currentRoute) {
        SettingsRoute.advancedRoute -> MainDestination.Settings.route
        else -> currentRoute
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
                            protectionEnabled = protectionEnabled,
                            activeModeSummary = activeModeSummary,
                            activeModeCount = activeModeCount,
                            monitoredAppsCount = monitoredApps.size,
                            blocksToday = blockCounts.today,
                            recentEvents = blockEvents.take(3),
                            onProtectionEnabledChange = { shouldEnable ->
                                protectionEnabled = shouldEnable
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
                            onClearHistoryConfirmed = {
                                blockEvents = emptyList()
                            },
                        )
                    }
                    composable(SettingsRoute.route) {
                        SettingsScreen(
                            monitoredApps = appConfigs,
                            selectedEngine = activeTextEngine,
                            globalLockdownDuration = globalLockdownDuration,
                            keywordBlocklist = keywordBlocklist,
                            deviceAdminEnabled = true,
                            onOpenAdvancedSettings = {
                                navController.navigate(SettingsRoute.advancedRoute)
                            },
                            onEngineSelected = { engine ->
                                activeTextEngine = engine
                            },
                            onMonitoredAppsUpdated = { selectedPackageNames ->
                                appConfigs = appConfigs.map { config ->
                                    config.copy(isMonitored = config.app.packageName in selectedPackageNames)
                                }
                            },
                            onKeywordAdded = { keyword ->
                                if (keyword.isNotBlank() && keyword !in keywordBlocklist) {
                                    keywordBlocklist = keywordBlocklist + keyword
                                }
                            },
                            onKeywordRemoved = { keyword ->
                                keywordBlocklist = keywordBlocklist - keyword
                            },
                            onLockdownDurationSelected = { duration ->
                                globalLockdownDuration = duration
                            },
                        )
                    }
                    composable(SettingsRoute.advancedRoute) {
                        AdvancedSettingsScreen(
                            appConfigs = monitoredApps,
                            supports640Model = supports640Model,
                            selectedVisualModel = activeVisualModel,
                            mode1Enabled = mode1OverrideEnabled,
                            mode2Enabled = mode2OverrideEnabled,
                            mode3Enabled = mode3OverrideEnabled,
                            frameSkipIntervalMs = frameSkipIntervalMs.toInt(),
                            topCapturePercent = topCapturePercent.toInt(),
                            middleCapturePercent = middleCapturePercent.toInt(),
                            onBack = { navController.popBackStack() },
                            onMode1Changed = { mode1OverrideEnabled = it },
                            onMode2Changed = { mode2OverrideEnabled = it },
                            onMode3Changed = { mode3OverrideEnabled = it },
                            onVisualModelSelected = { activeVisualModel = it },
                            onFrameSkipIntervalChanged = { frameSkipIntervalMs = it.toFloat() },
                            onTopCapturePercentChanged = { topCapturePercent = it.toFloat() },
                            onMiddleCapturePercentChanged = { middleCapturePercent = it.toFloat() },
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
            }
        }
    }
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
    val today = LocalDate.now()
    val weekStart = today.minusDays(6)
    return BlockCountSummary(
        today = events.count { it.timestamp.toLocalDate() == today },
        thisWeek = events.count { !it.timestamp.toLocalDate().isBefore(weekStart) },
        allTime = events.size,
    )
}

private fun deriveMostBlockedApp(
    events: List<BlockEventUiModel>,
): MostBlockedAppUiModel? {
    return events
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
