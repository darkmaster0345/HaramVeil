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

package com.haramveil.data.local

data class BlockEvent(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val triggerMode: Int,
    val detectionDetail: String,
    val timestamp: Long,
    val lockdownDurationMs: Long,
)

data class MostBlockedAppRecord(
    val packageName: String,
    val appName: String,
    val blockCount: Int,
)
