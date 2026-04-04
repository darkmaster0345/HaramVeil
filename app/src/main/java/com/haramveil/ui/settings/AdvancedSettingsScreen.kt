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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haramveil.data.models.VisualModelOption
import com.haramveil.ui.AppMonitoringUiModel
import com.haramveil.ui.HaramVeilSectionCard
import com.haramveil.ui.HaramVeilWordmarkHeader
import com.haramveil.ui.HaramVeilEmptyIllustration
import com.haramveil.ui.InstalledAppIcon
import com.haramveil.ui.LockdownDurationOption
import com.haramveil.ui.ModeOverrideOption
import com.haramveil.ui.SectionLabel
import com.haramveil.ui.StatusPill

@Composable
fun AdvancedSettingsScreen(
    appConfigs: List<AppMonitoringUiModel>,
    supports640Model: Boolean,
    selectedVisualModel: VisualModelOption,
    mode1Enabled: Boolean,
    mode2Enabled: Boolean,
    mode3Enabled: Boolean,
    frameSkipIntervalMs: Int,
    topCapturePercent: Int,
    middleCapturePercent: Int,
    onBack: () -> Unit,
    onMode1Changed: (Boolean) -> Unit,
    onMode2Changed: (Boolean) -> Unit,
    onMode3Changed: (Boolean) -> Unit,
    onVisualModelSelected: (VisualModelOption) -> Unit,
    onFrameSkipIntervalChanged: (Int) -> Unit,
    onTopCapturePercentChanged: (Int) -> Unit,
    onMiddleCapturePercentChanged: (Int) -> Unit,
    onModeOverrideChanged: (String, ModeOverrideOption) -> Unit,
    onLockdownOverrideChanged: (String, LockdownDurationOption) -> Unit,
) {
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
                title = "Advanced Settings",
                subtitle = "Independent mode switches, model controls, frame pacing, and per-app overrides stay local-only for now.",
                action = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        }
        item {
            SectionLabel(text = "Mode overrides")
        }
        item {
            HaramVeilSectionCard {
                AdvancedToggleRow(
                    label = "Mode 1",
                    description = "Static node scanning stays near-zero CPU and acts as the always-on first pass.",
                    checked = mode1Enabled,
                    onCheckedChange = onMode1Changed,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                AdvancedToggleRow(
                    label = "Mode 2",
                    description = "OCR can be toggled independently here even though the real cascade wiring arrives later.",
                    checked = mode2Enabled,
                    onCheckedChange = onMode2Changed,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                AdvancedToggleRow(
                    label = "Mode 3",
                    description = "Visual ONNX detection can be exposed independently for testing and per-device tuning.",
                    checked = mode3Enabled,
                    onCheckedChange = onMode3Changed,
                )
            }
        }
        item {
            SectionLabel(text = "Per-app overrides")
        }
        if (appConfigs.isEmpty()) {
            item {
                HaramVeilSectionCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HaramVeilEmptyIllustration()
                        Text(
                            text = "No monitored apps selected.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Pick apps in Settings first, then come back here for per-app mode and lockdown overrides.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(
                items = appConfigs,
                key = { config -> config.app.packageName },
            ) { config ->
                AppOverrideCard(
                    config = config,
                    onModeOverrideChanged = { option ->
                        onModeOverrideChanged(config.app.packageName, option)
                    },
                    onLockdownOverrideChanged = { option ->
                        onLockdownOverrideChanged(config.app.packageName, option)
                    },
                )
            }
        }
        item {
            SectionLabel(text = "Model and pacing")
        }
        item {
            HaramVeilSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ONNX model selector",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (supports640Model) {
                                "This device cleared the 640 benchmark, so both models are available."
                            } else {
                                "640 stays hidden because the benchmark only cleared 320 for this device."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilterChip(
                        selected = selectedVisualModel == VisualModelOption.MODEL_320,
                        onClick = { onVisualModelSelected(VisualModelOption.MODEL_320) },
                        label = { Text("320") },
                    )
                    if (supports640Model) {
                        FilterChip(
                            selected = selectedVisualModel == VisualModelOption.MODEL_640,
                            onClick = { onVisualModelSelected(VisualModelOption.MODEL_640) },
                            label = { Text("640") },
                        )
                    }
                }
                SliderControl(
                    label = "Frame skip interval",
                    description = "$frameSkipIntervalMs ms between visual scans",
                    value = frameSkipIntervalMs.toFloat(),
                    valueRange = 250f..2000f,
                    steps = 6,
                    onValueChange = { onFrameSkipIntervalChanged(it.toInt()) },
                )
            }
        }
        item {
            SectionLabel(text = "HaramClip zones")
        }
        item {
            HaramVeilSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = "Fine-tune the top and middle capture bands before OCR and ONNX phases are wired in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SliderControl(
                    label = "Top capture %",
                    description = "$topCapturePercent% of the screen",
                    value = topCapturePercent.toFloat(),
                    valueRange = 10f..50f,
                    steps = 7,
                    onValueChange = { onTopCapturePercentChanged(it.toInt()) },
                )
                SliderControl(
                    label = "Middle capture %",
                    description = "$middleCapturePercent% of the screen",
                    value = middleCapturePercent.toFloat(),
                    valueRange = 20f..60f,
                    steps = 7,
                    onValueChange = { onMiddleCapturePercentChanged(it.toInt()) },
                )
            }
        }
    }
}

@Composable
private fun AdvancedToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun AppOverrideCard(
    config: AppMonitoringUiModel,
    onModeOverrideChanged: (ModeOverrideOption) -> Unit,
    onLockdownOverrideChanged: (LockdownDurationOption) -> Unit,
) {
    HaramVeilSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InstalledAppIcon(
                app = config.app,
                size = 42.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = config.app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (config.app.isHighRisk) {
                        StatusPill(
                            label = "HIGH RISK",
                            backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Text(
                    text = config.app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OverrideSelectorRow(
            label = "Mode override",
            selectedValue = config.modeOverride.label,
            options = ModeOverrideOption.entries,
            labelMapper = { option -> option.label },
            onOptionSelected = onModeOverrideChanged,
        )
        OverrideSelectorRow(
            label = "Lockdown override",
            selectedValue = config.lockdownOverride.label,
            options = listOf(
                LockdownDurationOption.DEFAULT,
                LockdownDurationOption.MINUTES_5,
                LockdownDurationOption.MINUTES_15,
                LockdownDurationOption.MINUTES_30,
                LockdownDurationOption.HOUR_1,
                LockdownDurationOption.CUSTOM,
            ),
            labelMapper = { option -> option.label },
            onOptionSelected = onLockdownOverrideChanged,
        )
    }
}

@Composable
private fun <T> OverrideSelectorRow(
    label: String,
    selectedValue: String,
    options: List<T>,
    labelMapper: (T) -> String,
    onOptionSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labelMapper(option)) },
                        onClick = {
                            expanded = false
                            onOptionSelected(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}
