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

import com.haramveil.data.models.InstalledAppInfo
import java.time.LocalDateTime

enum class BlockDetectionMode(
    val label: String,
    val shortLabel: String,
) {
    MODE_1(
        label = "Mode 1",
        shortLabel = "M1",
    ),
    MODE_2(
        label = "Mode 2",
        shortLabel = "M2",
    ),
    MODE_3(
        label = "Mode 3",
        shortLabel = "M3",
    ),
}

enum class ModeOverrideOption(
    val label: String,
) {
    DEFAULT(label = "Default"),
    FORCE_MODE_1(label = "Force Mode 1"),
    FORCE_MODE_2(label = "Force Mode 2"),
    FORCE_MODE_3(label = "Force Mode 3"),
}

enum class LockdownDurationOption(
    val label: String,
    val minutes: Int?,
) {
    MINUTES_5(
        label = "5m",
        minutes = 5,
    ),
    MINUTES_15(
        label = "15m",
        minutes = 15,
    ),
    MINUTES_30(
        label = "30m",
        minutes = 30,
    ),
    HOUR_1(
        label = "1h",
        minutes = 60,
    ),
    CUSTOM(
        label = "Custom",
        minutes = null,
    ),
    DEFAULT(
        label = "Default",
        minutes = null,
    ),
}

data class BlockEventUiModel(
    val id: String,
    val app: InstalledAppInfo,
    val mode: BlockDetectionMode,
    val timestamp: LocalDateTime,
)

data class MostBlockedAppUiModel(
    val app: InstalledAppInfo,
    val count: Int,
)

data class AppMonitoringUiModel(
    val app: InstalledAppInfo,
    val isMonitored: Boolean,
    val modeOverride: ModeOverrideOption = ModeOverrideOption.DEFAULT,
    val lockdownOverride: LockdownDurationOption = LockdownDurationOption.DEFAULT,
)
