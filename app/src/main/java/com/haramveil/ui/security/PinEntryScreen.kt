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

package com.haramveil.ui.security

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haramveil.ui.HaramVeilSectionCard
import com.haramveil.ui.StatusPill

@Composable
fun PinEntryScreen(
    title: String,
    subtitle: String,
    enteredPin: String,
    confirmLabel: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isLockedOut: Boolean = false,
    lockoutRemainingMs: Long = 0L,
    showForgotPin: Boolean = false,
    shakeTrigger: Int = 0,
    cancelLabel: String? = null,
    onDigitPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    onForgotPin: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger <= 0) {
            return@LaunchedEffect
        }

        listOf(-24f, 24f, -16f, 16f, -8f, 8f, 0f).forEach { offset ->
            shakeOffset.animateTo(
                targetValue = offset,
                animationSpec = tween(durationMillis = 40),
            )
        }
    }

    HaramVeilSectionCard(
        modifier = modifier,
        contentPadding = PaddingValues(22.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isLockedOut) {
                    StatusPill(
                        label = "Try again in ${formatLockoutCountdown(lockoutRemainingMs)}",
                        backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                }
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            PinDotsRow(
                pinLength = enteredPin.length,
                modifier = Modifier.graphicsLayer {
                    translationX = shakeOffset.value
                },
            )

            PinNumpad(
                enabled = !isLockedOut,
                onDigitPressed = onDigitPressed,
                onClear = onClear,
                onBackspace = onBackspace,
            )

            Button(
                onClick = onConfirm,
                enabled = enteredPin.length == RequiredPinLength && !isLockedOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(confirmLabel)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showForgotPin && onForgotPin != null) {
                    TextButton(onClick = onForgotPin) {
                        Text("Forgot PIN?")
                    }
                } else {
                    Box(modifier = Modifier.size(1.dp))
                }

                if (cancelLabel != null && onCancel != null) {
                    OutlinedButton(onClick = onCancel) {
                        Text(cancelLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDotsRow(
    pinLength: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(RequiredPinLength) { index ->
            Surface(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(18.dp),
                shape = CircleShape,
                color = if (index < pinLength) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = if (index < pinLength) 6.dp else 0.dp,
            ) {}
        }
    }
}

@Composable
private fun PinNumpad(
    enabled: Boolean,
    onDigitPressed: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        ).forEach { digits ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                digits.forEach { digit ->
                    NumpadButton(
                        label = digit,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        onClick = { onDigitPressed(digit) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NumpadButton(
                label = "Clear",
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = onClear,
            )
            NumpadButton(
                label = "0",
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { onDigitPressed("0") },
            )
            NumpadButton(
                label = "Back",
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun NumpadButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        tonalElevation = if (enabled) 6.dp else 0.dp,
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

internal fun formatLockoutCountdown(
    remainingMs: Long,
): String {
    val totalSeconds = (remainingMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

internal const val RequiredPinLength = 6
