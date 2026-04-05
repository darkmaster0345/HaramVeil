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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haramveil.data.models.SecurityQuestion
import com.haramveil.ui.HaramVeilSectionCard
import com.haramveil.ui.StatusPill

@Composable
fun SecurityQuestionScreen(
    title: String,
    subtitle: String,
    questions: List<SecurityQuestion>,
    selectedAnswers: Map<Int, Boolean>,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isLockedOut: Boolean = false,
    lockoutRemainingMs: Long = 0L,
    submitLabel: String = "Submit answers",
    backLabel: String = "Back",
    onAnswerSelected: (Int, Boolean) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
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

            if (questions.isEmpty()) {
                Text(
                    text = "Recovery questions are not configured on this device yet, so PIN reset is unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                questions.forEachIndexed { index, question ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = question.prompt,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = selectedAnswers[index] == true,
                                onClick = { onAnswerSelected(index, true) },
                                enabled = !isLockedOut,
                                label = { Text("Yes") },
                            )
                            FilterChip(
                                selected = selectedAnswers[index] == false,
                                onClick = { onAnswerSelected(index, false) },
                                enabled = !isLockedOut,
                                label = { Text("No") },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onBack) {
                    Text(backLabel)
                }
                Button(
                    onClick = onSubmit,
                    enabled = questions.isNotEmpty() &&
                        selectedAnswers.size == questions.size &&
                        !isLockedOut,
                ) {
                    Text(submitLabel)
                }
            }
        }
    }
}
