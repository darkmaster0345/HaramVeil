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

package com.haramveil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HaramVeilDarkColorScheme = darkColorScheme(
    primary = Color(0xFF1B4332),
    onPrimary = Color(0xFFF6FFF8),
    primaryContainer = Color(0xFF24543F),
    onPrimaryContainer = Color(0xFFE2F6E8),
    secondary = Color(0xFF7BCFA7),
    onSecondary = Color(0xFF082117),
    secondaryContainer = Color(0xFF113727),
    onSecondaryContainer = Color(0xFFCBEEDC),
    tertiary = Color(0xFF4FC3C8),
    onTertiary = Color(0xFF07282B),
    background = Color(0xFF0D1B2A),
    onBackground = Color(0xFFE7F1F7),
    surface = Color(0xFF132537),
    onSurface = Color(0xFFE7F1F7),
    surfaceVariant = Color(0xFF183244),
    onSurfaceVariant = Color(0xFFBDD0DD),
    outline = Color(0xFF406378),
    error = Color(0xFFF4A261),
)

@Composable
fun HaramVeilTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = HaramVeilDarkColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
