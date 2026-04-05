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

package com.haramveil.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haramveil.ui.BlockDetectionMode
import com.haramveil.ui.BlockEventUiModel
import com.haramveil.ui.HaramVeilEmptyIllustration
import com.haramveil.ui.HaramVeilSectionCard
import com.haramveil.ui.HaramVeilWordmarkHeader
import com.haramveil.ui.InstalledAppIcon
import com.haramveil.ui.ModeBadge
import com.haramveil.ui.MostBlockedAppUiModel
import com.haramveil.ui.SectionLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    events: List<BlockEventUiModel>,
    todayCount: Int,
    thisWeekCount: Int,
    allTimeCount: Int,
    mostBlockedApp: MostBlockedAppUiModel?,
    onRequestClearHistory: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf<BlockDetectionMode?>(null) }
    val filteredEvents = remember(events, selectedFilter) {
        events.filter { selectedFilter == null || it.mode == selectedFilter }
    }

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
                title = "Activity Log",
                subtitle = "Encrypted on-device history of recent interventions, filter hits, and visual detections.",
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatsSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Today",
                    count = todayCount,
                )
                StatsSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "This Week",
                    count = thisWeekCount,
                )
                StatsSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "All Time",
                    count = allTimeCount,
                )
            }
        }
        item {
            HaramVeilSectionCard {
                SectionLabel(text = "Most blocked app")
                if (mostBlockedApp == null) {
                    Text(
                        text = "No block history yet. Once HaramVeil steps in, the app most often interrupted will surface here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        InstalledAppIcon(
                            app = mostBlockedApp.app,
                            size = 50.dp,
                        )
                        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mostBlockedApp.app.label,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${mostBlockedApp.count} total blocks recorded locally",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.QueryStats,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        item {
            HaramVeilSectionCard {
                SectionLabel(text = "Filter by detection mode")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All") },
                    )
                    listOf(
                        BlockDetectionMode.MODE_1,
                        BlockDetectionMode.MODE_2,
                        BlockDetectionMode.MODE_3,
                    ).forEach { mode ->
                        FilterChip(
                            selected = selectedFilter == mode,
                            onClick = { selectedFilter = mode },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
        }
        item {
            SectionLabel(text = "Block events")
        }
        if (filteredEvents.isEmpty()) {
            item {
                HaramVeilSectionCard {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HaramVeilEmptyIllustration()
                        Text(
                            text = "All clear. Stay strong.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "This list is empty for the current filter. That can mean no recent blocks or a freshly cleared history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredEvents,
                key = { event -> event.id },
            ) { event ->
                StatsEventRow(event = event)
            }
        }
        item {
            OutlinedButton(
                onClick = onRequestClearHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFF28B82),
                )
                Text(
                    text = "Clear History",
                    color = Color(0xFFF28B82),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    HaramVeilSectionCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatsEventRow(
    event: BlockEventUiModel,
) {
    val timestampFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    }
    val weekdayFormatter = remember {
        DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault())
    }
    val formattedTimestamp = remember(event.timestamp) {
        val eventDate = event.timestamp.toLocalDate()
        if (eventDate == LocalDate.now()) {
            "Today ${event.timestamp.format(timestampFormatter)}"
        } else {
            event.timestamp.format(weekdayFormatter)
        }
    }

    HaramVeilSectionCard(
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InstalledAppIcon(
                app = event.app,
                size = 46.dp,
            )
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formattedTimestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDetectionDetail(event.detectionDetail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ModeBadge(mode = event.mode)
        }
    }
}

private fun formatDetectionDetail(
    detail: String,
): String {
    return when (detail) {
        "visual_content" -> "Visual content detected"
        "lockdown_retrigger" -> "Lockdown timer was reset after the app reopened"
        else -> "Matched keyword: $detail"
    }
}
