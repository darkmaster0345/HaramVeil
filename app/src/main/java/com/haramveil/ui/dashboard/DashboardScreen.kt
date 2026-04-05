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

package com.haramveil.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haramveil.ui.ActiveLockdownUiModel
import com.haramveil.ui.BlockEventUiModel
import com.haramveil.ui.HaramVeilEmptyIllustration
import com.haramveil.ui.HaramVeilSectionCard
import com.haramveil.ui.HaramVeilWordmarkHeader
import com.haramveil.ui.InstalledAppIcon
import com.haramveil.ui.ModeBadge
import com.haramveil.ui.SectionLabel
import com.haramveil.ui.StatusPill
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    protectionEnabled: Boolean,
    activeModeSummary: String,
    activeModeCount: Int,
    monitoredAppsCount: Int,
    blocksToday: Int,
    accessibilityServiceActive: Boolean,
    deviceAdminEnabled: Boolean,
    foregroundServiceActive: Boolean,
    rootModeEnabled: Boolean,
    activeLockdowns: List<ActiveLockdownUiModel>,
    batteryOptimizationCompleted: Boolean,
    batteryOptimizationManufacturerLabel: String,
    recentEvents: List<BlockEventUiModel>,
    onProtectionEnabledChange: (Boolean) -> Unit,
    onRequestProtectionDisable: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatteryOptimizationGuide: () -> Unit,
    onMarkBatteryOptimizationCompleted: () -> Unit,
    onViewFullStats: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                HaramVeilWordmarkHeader(
                    title = "Quiet protection, ready in the background",
                    subtitle = "A calm snapshot of your monitoring coverage, recent blocks, and current protection state.",
                )
            }
            item {
                HaramVeilSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Protection ${if (protectionEnabled) "ON" else "OFF"}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (protectionEnabled) {
                                    "HaramVeil is actively watching for risky content across your monitored apps."
                                } else {
                                    "Protection is paused. A PIN confirmation is required before this state changes."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = protectionEnabled,
                            onCheckedChange = { shouldEnable ->
                                if (shouldEnable) {
                                    onProtectionEnabledChange(true)
                                } else {
                                    onRequestProtectionDisable()
                                }
                            },
                        )
                    }
                    StatusPill(
                        label = if (protectionEnabled) "PIN required to switch off" else "Currently paused",
                        backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Accessibility Service: ${if (accessibilityServiceActive) "Active" else "Inactive"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        if (!accessibilityServiceActive) {
                            TextButton(onClick = onOpenAccessibilitySettings) {
                                Text("Enable")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatusPill(
                            label = if (deviceAdminEnabled) "Device Admin enabled" else "Device Admin off",
                            backgroundColor = if (deviceAdminEnabled) {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            },
                            contentColor = if (deviceAdminEnabled) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        StatusPill(
                            label = if (foregroundServiceActive) "Foreground service active" else "Foreground service restarting",
                            backgroundColor = if (foregroundServiceActive) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            },
                            contentColor = if (foregroundServiceActive) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        if (rootModeEnabled) {
                            StatusPill(
                                label = "Root mode: Enhanced Protection",
                                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            item {
                BoxWithConstraints {
                    if (maxWidth < 440.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DashboardMetricCard(
                                title = "Active Mode",
                                value = "$activeModeCount live",
                                supporting = activeModeSummary,
                            )
                            DashboardMetricCard(
                                title = "Apps Monitored",
                                value = monitoredAppsCount.toString(),
                                supporting = "Selected during setup",
                            )
                            DashboardMetricCard(
                                title = "Blocks Today",
                                value = blocksToday.toString(),
                                supporting = "Recent interventions",
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DashboardMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Active Mode",
                                value = "$activeModeCount live",
                                supporting = activeModeSummary,
                            )
                            DashboardMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Apps Monitored",
                                value = monitoredAppsCount.toString(),
                                supporting = "Selected during setup",
                            )
                            DashboardMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Blocks Today",
                                value = blocksToday.toString(),
                                supporting = "Recent interventions",
                            )
                        }
                    }
                }
            }
            if (!batteryOptimizationCompleted) {
                item {
                    HaramVeilSectionCard {
                        SectionLabel(text = "Battery protection step")
                        Text(
                            text = "Finish the ${batteryOptimizationManufacturerLabel} battery setup so background protection is less likely to be killed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = onOpenBatteryOptimizationGuide,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Open guide")
                            }
                            Button(
                                onClick = onMarkBatteryOptimizationCompleted,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("I've done this")
                            }
                        }
                    }
                }
            }
            if (activeLockdowns.isNotEmpty()) {
                item {
                    HaramVeilSectionCard {
                        SectionLabel(text = "Active lockdowns")
                        activeLockdowns.forEach { lockdown ->
                            ActiveLockdownRow(lockdown = lockdown)
                        }
                    }
                }
            }
            item {
                HaramVeilSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionLabel(text = "Recent block activity")
                        TextButton(onClick = onViewFullStats) {
                            Text("View Full Stats")
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                            )
                        }
                    }
                    if (recentEvents.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            HaramVeilEmptyIllustration()
                            Text(
                                text = "No recent blocks yet.",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Once protection starts intervening, the latest three events will appear here.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        recentEvents.forEach { event ->
                            QuickEventRow(event = event)
                        }
                    }
                }
            }
            item {
                HaramVeilSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Column {
                            Text(
                                text = "Lower the gaze, protect the heart",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "The veil, lockdown timer, and encrypted local activity log are all active on this device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveLockdownRow(
    lockdown: ActiveLockdownUiModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstalledAppIcon(
            app = lockdown.app,
            size = 42.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lockdown.app.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = lockdown.app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(
            label = lockdown.remainingLabel,
            backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    HaramVeilSectionCard(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun QuickEventRow(
    event: BlockEventUiModel,
) {
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstalledAppIcon(
            app = event.app,
            size = 42.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.app.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = event.timestamp.format(timeFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ModeBadge(mode = event.mode)
    }
}
