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

package com.haramveil.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.haramveil.data.models.TextRecognitionEngine
import com.haramveil.ui.AppMonitoringUiModel
import com.haramveil.ui.HaramVeilSectionCard
import com.haramveil.ui.HaramVeilWordmarkHeader
import com.haramveil.ui.InstalledAppIcon
import com.haramveil.ui.LockdownDurationOption
import com.haramveil.ui.SectionLabel
import com.haramveil.ui.StatusPill

@Composable
fun SettingsScreen(
    monitoredApps: List<AppMonitoringUiModel>,
    selectedEngine: TextRecognitionEngine,
    globalLockdownDuration: LockdownDurationOption,
    keywordBlocklist: List<String>,
    deviceAdminEnabled: Boolean,
    onOpenAdvancedSettings: () -> Unit,
    onOpenChangePin: () -> Unit,
    onEngineSelected: (TextRecognitionEngine) -> Unit,
    onMonitoredAppsUpdated: (Set<String>) -> Unit,
    onKeywordAdded: (String) -> Unit,
    onKeywordRemoved: (String) -> Unit,
    onLockdownDurationSelected: (LockdownDurationOption) -> Unit,
) {
    val context = LocalContext.current
    val versionLabel = remember(context) {
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        }.getOrDefault("0.1.0")
    }
    var draftKeyword by rememberSaveable { mutableStateOf("") }
    var showEngineDialog by remember { mutableStateOf(false) }
    var showAppsDialog by remember { mutableStateOf(false) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }
    val monitoredCount = monitoredApps.count { it.isMonitored }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HaramVeilWordmarkHeader(
                title = "Settings",
                subtitle = "Tune protection coverage, lockdown behavior, and the local security controls that guard HaramVeil itself.",
            )
        }
        item {
            SectionLabel(text = "PROTECTION")
        }
        item {
            HaramVeilSectionCard {
                SettingsActionRow(
                    title = "Engine",
                    supporting = selectedEngine.displayName,
                    onClick = { showEngineDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                SettingsActionRow(
                    title = "Monitored Apps",
                    supporting = "$monitoredCount apps selected",
                    onClick = { showAppsDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Keyword Blocklist",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Words and regex patterns stay local. Later phases will encrypt and enforce them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = draftKeyword,
                            onValueChange = { draftKeyword = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Add word or regex") },
                        )
                        Button(
                            onClick = {
                                if (draftKeyword.isNotBlank()) {
                                    onKeywordAdded(draftKeyword.trim())
                                    draftKeyword = ""
                                }
                            },
                        ) {
                            Text("Add")
                        }
                    }
                    if (keywordBlocklist.isEmpty()) {
                        Text(
                            text = "No custom entries yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        keywordBlocklist.forEach { keyword ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = keyword,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { onKeywordRemoved(keyword) }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Lockdown Duration",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        listOf(
                            LockdownDurationOption.MINUTES_5,
                            LockdownDurationOption.MINUTES_15,
                            LockdownDurationOption.MINUTES_30,
                            LockdownDurationOption.HOUR_1,
                            LockdownDurationOption.CUSTOM,
                        ).forEach { option ->
                            FilterChip(
                                selected = globalLockdownDuration == option,
                                onClick = { onLockdownDurationSelected(option) },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionLabel(text = "SECURITY")
        }
        item {
            HaramVeilSectionCard {
                SettingsActionRow(
                    title = "Change PIN",
                    supporting = "Verify the current PIN, then save a new bcrypt hash",
                    onClick = onOpenChangePin,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                SettingsActionRow(
                    title = "Change Security Questions",
                    supporting = "Your recovery answers stay hashed only. Rotation will follow the same secure flow.",
                    onClick = {
                        infoDialogMessage = "Recovery question rotation is not fully separate yet. Today, the forgot-PIN flow can still reset the PIN securely using the questions already saved on-device."
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Device Admin Status",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "This will matter later for uninstall resistance and service hardening.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        label = if (deviceAdminEnabled) "Enabled" else "Disabled",
                        backgroundColor = if (deviceAdminEnabled) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                        } else {
                            Color(0xFFF28B82).copy(alpha = 0.16f)
                        },
                        contentColor = if (deviceAdminEnabled) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            Color(0xFFF28B82)
                        },
                    )
                }
            }
        }
        item {
            SectionLabel(text = "ABOUT")
        }
        item {
            HaramVeilSectionCard {
                AboutRow(
                    icon = Icons.Outlined.Code,
                    title = "Version",
                    supporting = versionLabel,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                AboutRow(
                    icon = Icons.Outlined.Security,
                    title = "License",
                    supporting = "GPL-v3",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                SettingsActionRow(
                    title = "GitHub",
                    supporting = "github.com/darkmaster0345/HaramVeil",
                    onClick = {
                        infoDialogMessage = "GitHub deep linking will be polished later. The repository URL is already reserved here in the UI."
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                SettingsActionRow(
                    title = "Advanced Settings",
                    supporting = "Per-app overrides, model choice, frame skip, and HaramClip zones",
                    onClick = onOpenAdvancedSettings,
                )
            }
        }
    }

    if (showEngineDialog) {
        EngineSelectionDialog(
            selectedEngine = selectedEngine,
            onDismiss = { showEngineDialog = false },
            onSelect = { engine ->
                showEngineDialog = false
                onEngineSelected(engine)
            },
        )
    }

    if (showAppsDialog) {
        MonitoredAppsDialog(
            apps = monitoredApps,
            onDismiss = { showAppsDialog = false },
            onSave = { selectedPackageNames ->
                showAppsDialog = false
                onMonitoredAppsUpdated(selectedPackageNames)
            },
        )
    }

    if (infoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { infoDialogMessage = null },
            title = { Text("Coming in a later phase") },
            text = { Text(infoDialogMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { infoDialogMessage = null }) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    supporting: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    supporting: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(12.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EngineSelectionDialog(
    selectedEngine: TextRecognitionEngine,
    onDismiss: () -> Unit,
    onSelect: (TextRecognitionEngine) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose text engine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    TextRecognitionEngine.ML_KIT to "Faster on Google-equipped phones.",
                    TextRecognitionEngine.FOSS_ONNX to "Better fit for de-Googled or fully FOSS setups.",
                ).forEach { (engine, note) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(engine) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedEngine == engine,
                            onClick = { onSelect(engine) },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = engine.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun MonitoredAppsDialog(
    apps: List<AppMonitoringUiModel>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit,
) {
    var selectedPackages by remember(apps) {
        mutableStateOf(
            apps.filter { it.isMonitored }.map { it.app.packageName }.toSet(),
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Monitored apps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "This reuses the onboarding idea, but keeps the edit local to the Phase 3 UI for now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = apps,
                        key = { item -> item.app.packageName },
                    ) { appConfig ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPackages = selectedPackages.toggle(appConfig.app.packageName)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Checkbox(
                                checked = appConfig.app.packageName in selectedPackages,
                                onCheckedChange = {
                                    selectedPackages = selectedPackages.toggle(appConfig.app.packageName)
                                },
                            )
                            InstalledAppIcon(
                                app = appConfig.app,
                                size = 40.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = appConfig.app.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (appConfig.app.isHighRisk) {
                                        StatusPill(
                                            label = "HIGH RISK",
                                            backgroundColor = Color(0xFFF6C065).copy(alpha = 0.16f),
                                            contentColor = Color(0xFFF6C065),
                                        )
                                    }
                                }
                                Text(
                                    text = appConfig.app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(selectedPackages) },
                        modifier = Modifier.padding(start = 10.dp),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun Set<String>.toggle(packageName: String): Set<String> {
    return if (packageName in this) {
        this - packageName
    } else {
        this + packageName
    }
}
