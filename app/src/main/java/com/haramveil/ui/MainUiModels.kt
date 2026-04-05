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
    SYSTEM_ALERT(
        label = "System Alert",
        shortLabel = "SYS",
    ),
    ;

    companion object {
        fun fromTriggerMode(triggerMode: Int): BlockDetectionMode =
            when (triggerMode) {
                1 -> MODE_1
                2 -> MODE_2
                3 -> MODE_3
                else -> SYSTEM_ALERT
            }
    }
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
    ;

    fun toDurationMs(
        fallbackDurationMs: Long = DefaultCustomDurationMs,
    ): Long = when (this) {
        MINUTES_5 -> 5 * 60 * 1_000L
        MINUTES_15 -> 15 * 60 * 1_000L
        MINUTES_30 -> 30 * 60 * 1_000L
        HOUR_1 -> 60 * 60 * 1_000L
        CUSTOM,
        DEFAULT,
        -> fallbackDurationMs
    }

    companion object {
        const val DefaultCustomDurationMs = 45 * 60 * 1_000L

        fun fromDurationMs(durationMs: Long): LockdownDurationOption =
            when (durationMs) {
                MINUTES_5.toDurationMs() -> MINUTES_5
                MINUTES_15.toDurationMs() -> MINUTES_15
                MINUTES_30.toDurationMs() -> MINUTES_30
                HOUR_1.toDurationMs() -> HOUR_1
                else -> CUSTOM
            }
    }
}

data class BlockEventUiModel(
    val id: String,
    val app: InstalledAppInfo,
    val mode: BlockDetectionMode,
    val timestamp: LocalDateTime,
    val detectionDetail: String,
)

data class MostBlockedAppUiModel(
    val app: InstalledAppInfo,
    val count: Int,
)

data class ActiveLockdownUiModel(
    val app: InstalledAppInfo,
    val remainingDurationMs: Long,
    val remainingLabel: String,
)

data class AppMonitoringUiModel(
    val app: InstalledAppInfo,
    val isMonitored: Boolean,
    val modeOverride: ModeOverrideOption = ModeOverrideOption.DEFAULT,
    val lockdownOverride: LockdownDurationOption = LockdownDurationOption.DEFAULT,
)
