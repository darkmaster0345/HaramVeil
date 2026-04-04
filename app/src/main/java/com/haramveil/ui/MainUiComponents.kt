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

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.text.KeyboardOptions
import com.haramveil.data.models.InstalledAppInfo

@Composable
fun HaramVeilScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07121D),
                        MaterialTheme.colorScheme.background,
                        Color(0xFF133227),
                    ),
                ),
            ),
    ) {
        DecorativeOverlay()
        content()
    }
}

@Composable
fun DecorativeOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.16f },
    ) {
        Box(
            modifier = Modifier
                .padding(start = 228.dp, top = 70.dp)
                .size(160.dp)
                .rotate(45f)
                .border(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(28.dp),
                ),
        )
        Box(
            modifier = Modifier
                .padding(start = 32.dp, top = 148.dp)
                .size(88.dp)
                .rotate(15f)
                .border(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(22.dp),
                ),
        )
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 540.dp)
                .size(138.dp)
                .rotate(20f)
                .border(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)),
                    shape = RoundedCornerShape(32.dp),
                ),
        )
    }
}

@Composable
fun HaramVeilWordmarkHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                HaramVeilWordmarkIcon()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "HaramVeil",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (action != null) {
                    action()
                }
            }
            GeometricPatternBand()
        }
    }
}

@Composable
fun HaramVeilWordmarkIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .rotate(45f)
                .border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f)),
                    shape = RoundedCornerShape(18.dp),
                ),
        )
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(18.dp),
                ),
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary,
                        ),
                    ),
                ),
        )
    }
}

@Composable
fun GeometricPatternBand(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp)
                    .rotate(if (index % 2 == 0) 45f else 0f)
                    .border(
                        border = BorderStroke(
                            1.dp,
                            if (index % 2 == 0) {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.40f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                            },
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ),
            )
        }
    }
}

@Composable
fun HaramVeilSectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun SectionLabel(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun StatusPill(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun ModeBadge(
    mode: BlockDetectionMode,
) {
    val accentColor = when (mode) {
        BlockDetectionMode.MODE_1 -> MaterialTheme.colorScheme.secondary
        BlockDetectionMode.MODE_2 -> Color(0xFF8BE1BA)
        BlockDetectionMode.MODE_3 -> MaterialTheme.colorScheme.tertiary
    }
    StatusPill(
        label = mode.label,
        backgroundColor = accentColor.copy(alpha = 0.15f),
        contentColor = accentColor,
    )
}

@Composable
fun InstalledAppIcon(
    app: InstalledAppInfo,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val context = LocalContext.current
    val iconDrawable by produceState<Drawable?>(initialValue = null, app.packageName) {
        value = runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
        }.getOrNull()
    }

    if (iconDrawable != null) {
        Image(
            bitmap = iconDrawable!!.toBitmap(72, 72).asImageBitmap(),
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
fun PlaceholderPinDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { updated ->
                        pin = updated.filter(Char::isDigit).take(6)
                    },
                    singleLine = true,
                    label = { Text("Enter 6-digit PIN") },
                    placeholder = { Text("••••••") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Text(
                    text = "Phase 8 will connect this dialog to the stored PIN check. For now, any 6 digits unlock the UI flow.",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pin) },
                enabled = pin.length == 6,
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun HaramVeilEmptyIllustration(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size((120 - (index * 18)).dp)
                    .rotate((index * 22).toFloat())
                    .border(
                        border = BorderStroke(
                            1.dp,
                            if (index == 1) {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.48f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            },
                        ),
                        shape = RoundedCornerShape(28.dp),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)),
        )
    }
}
