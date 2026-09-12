/*
 * Copyright 2026 Boyuan Yang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.byang.eventsince.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.byang.eventsince.R
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.core.time.Heatmap
import me.byang.eventsince.core.time.HeatmapCell
import me.byang.eventsince.core.time.HeatmapWeek
import me.byang.eventsince.ui.common.formatDate
import me.byang.eventsince.ui.theme.LocalIsDarkTheme
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.TextStyle

private const val LIGHT_EMPTY = 0xFFF3F3F3.toInt()
private const val LIGHT_ONE = 0xFFD6D6D6.toInt()
private const val LIGHT_FULL = 0xFF000000.toInt()
private const val DARK_EMPTY = 0xFF222222.toInt()
private const val DARK_ONE = 0xFF414141.toInt()
private const val DARK_FULL = 0xFFFFFFFF.toInt()

/** Vertical GitHub-style heatmap: newest week on top, one row per week. */
@Composable
fun HeatmapView(
    weeks: List<HeatmapWeek>,
    weekStart: DayOfWeek,
    dateFormat: DateFormatPreference,
    modifier: Modifier = Modifier,
    onCellClick: (HeatmapCell) -> Unit = {},
) {
    val dark = LocalIsDarkTheme.current
    val locale = LocalConfiguration.current.locales[0]
    val max = remember(weeks) { weeks.maxOf { w -> w.cells.maxOf { it?.count ?: 0 } } }
    var selected by remember { mutableStateOf<HeatmapCell?>(null) }
    val cellSize = 22.dp
    val gap = 4.dp
    val labelWidth = 44.dp
    val density = LocalDensity.current
    val cellPx = with(density) { cellSize.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = labelWidth)) {
            Heatmap.weekdays(weekStart).forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(cellSize + gap),
                )
            }
        }
        val rows = weeks.size
        val heightDp = cellSize * rows + gap * (rows - 1).coerceAtLeast(0)
        Row(modifier = Modifier.fillMaxWidth().height(heightDp)) {
            Column(modifier = Modifier.width(labelWidth)) {
                weeks.forEach { week ->
                    Text(
                        text = week.monthLabel?.month?.getDisplayName(TextStyle.SHORT, locale) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        modifier = Modifier.height(cellSize + gap),
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .width((cellSize + gap) * 7)
                    .height(heightDp)
                    .pointerInput(weeks) {
                        detectTapGestures { offset ->
                            val col = (offset.x / (cellPx + gapPx)).toInt()
                            val row = (offset.y / (cellPx + gapPx)).toInt()
                            val cell = weeks.getOrNull(row)?.cells?.getOrNull(col)
                            // Only days with at least one reset react: show the count and jump to the record.
                            if (cell != null && cell.count > 0) {
                                selected = if (cell != selected) cell else null
                                onCellClick(cell)
                            } else {
                                selected = null
                            }
                        }
                    },
            ) {
                weeks.forEachIndexed { row, week ->
                    week.cells.forEachIndexed { col, cell ->
                        if (cell == null) return@forEachIndexed
                        val argb = if (dark) Heatmap.cellColor(cell.count, max, DARK_EMPTY, DARK_ONE, DARK_FULL)
                        else Heatmap.cellColor(cell.count, max, LIGHT_EMPTY, LIGHT_ONE, LIGHT_FULL)
                        drawRoundRect(
                            color = Color(argb),
                            topLeft = Offset(col * (cellPx + gapPx), row * (cellPx + gapPx)),
                            size = Size(cellPx, cellPx),
                            cornerRadius = CornerRadius(cellPx * 0.2f),
                        )
                    }
                }
            }
        }
        selected?.let { cell ->
            Card(modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val millis = cell.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    Text(formatDate(millis, dateFormat), style = MaterialTheme.typography.labelLarge)
                    Text(pluralStringResource(R.plurals.history_resets_on_day, cell.count, cell.count), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
