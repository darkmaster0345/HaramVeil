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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.haramveil.ui.BlockEventUiModel
import com.haramveil.ui.HaramVeilSectionCard
import com.haramveil.ui.HaramVeilWordmarkHeader
import com.haramveil.ui.InstalledAppIcon
import com.haramveil.ui.ModeBadge
import com.haramveil.ui.PlaceholderPinDialog
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
    recentEvents: List<BlockEventUiModel>,
    onProtectionEnabledChange: (Boolean) -> Unit,
    onViewFullStats: () -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }

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
                                    showPinDialog = true
                                }
                            },
                        )
                    }
                    StatusPill(
                        label = if (protectionEnabled) "PIN required to switch off" else "Currently paused",
                        backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            item {
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
                        Text(
                            text = "No recent blocks yet. Once protection starts intervening, the latest three events will appear here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
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
                                text = "This Phase 3 dashboard is UI-only. Detection, lockdown enforcement, and real stats storage will be wired in later phases.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        PlaceholderPinDialog(
            title = "Turn protection off?",
            message = "A PIN gate belongs here so HaramVeil cannot be disabled casually or in a weak moment.",
            confirmLabel = "Turn Off",
            onDismiss = { showPinDialog = false },
            onConfirm = {
                showPinDialog = false
                onProtectionEnabledChange(false)
            },
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
