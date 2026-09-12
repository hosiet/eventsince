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
package me.byang.eventsince.core.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HeatmapCell(val date: LocalDate, val count: Int)

/** One row of the heatmap: seven slots starting on the configured first day of the week. */
data class HeatmapWeek(
    val start: LocalDate,
    /** Null for days outside the covered range. */
    val cells: List<HeatmapCell?>,
    /** Set when this row should carry a month label. */
    val monthLabel: LocalDate?,
)

/** Groups reset instants by local calendar day and lays them out in weeks, newest first. */
object Heatmap {

    fun countsByDay(instants: Collection<Long>, zone: ZoneId): Map<LocalDate, Int> =
        instants.groupingBy { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.eachCount()

    fun build(
        instants: Collection<Long>,
        rangeStartMillis: Long,
        today: LocalDate,
        weekStart: DayOfWeek,
        zone: ZoneId,
    ): List<HeatmapWeek> {
        val counts = countsByDay(instants, zone)
        val earliest = Instant.ofEpochMilli(rangeStartMillis).atZone(zone).toLocalDate()
        val rangeStart = minOf(earliest, counts.keys.minOrNull() ?: earliest)
        val firstWeek = rangeStart.with(TemporalAdjusters.previousOrSame(weekStart))
        val lastWeek = today.with(TemporalAdjusters.previousOrSame(weekStart))

        val weeks = mutableListOf<HeatmapWeek>()
        var weekStartDate = lastWeek
        while (!weekStartDate.isBefore(firstWeek)) {
            val cells = (0 until 7).map { offset ->
                val day = weekStartDate.plusDays(offset.toLong())
                if (day.isBefore(rangeStart) || day.isAfter(today)) null else HeatmapCell(day, counts[day] ?: 0)
            }
            weeks += HeatmapWeek(weekStartDate, cells, monthLabel = null)
            weekStartDate = weekStartDate.minusWeeks(1)
        }
        // Label a row when its month differs from the older row below it (or it is the oldest row).
        return weeks.mapIndexed { index, week ->
            val older = weeks.getOrNull(index + 1)
            val label = if (older == null || older.start.month != week.start.month || older.start.year != week.start.year) week.start else null
            week.copy(monthLabel = label)
        }
    }

    /** Weekday order for a row, starting on [weekStart]. */
    fun weekdays(weekStart: DayOfWeek): List<DayOfWeek> = (0 until 7).map { weekStart.plus(it.toLong()) }

    /** Linear interpolation between two ARGB colours, t in 0..1. */
    fun lerpColor(from: Int, to: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun ch(shift: Int): Int {
            val a = from shr shift and 0xFF
            val b = to shr shift and 0xFF
            return (a + (b - a) * k + 0.5f).toInt()
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /**
     * Colour for a cell with [count] resets when the busiest day has [max]. Zero stays on the
     * empty colour; otherwise count / max is interpolated between the lightest and darkest tones.
     */
    fun cellColor(count: Int, max: Int, empty: Int, one: Int, full: Int): Int {
        if (count <= 0) return empty
        if (max <= 1) return full
        return lerpColor(one, full, count.toFloat() / max)
    }
}
