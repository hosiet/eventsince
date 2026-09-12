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

import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeatmapTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private fun ms(date: LocalDate, hour: Int = 12) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `groups resets by local day and lays out weeks newest first`() {
        val today = LocalDate.of(2024, 3, 20) // Wednesday
        val created = LocalDate.of(2024, 3, 5)
        val resets = listOf(ms(LocalDate.of(2024, 3, 8)), ms(LocalDate.of(2024, 3, 8), 23), ms(LocalDate.of(2024, 3, 19)))
        val weeks = Heatmap.build(resets, ms(created), today, DayOfWeek.MONDAY, zone)

        assertEquals(3, weeks.size)
        assertEquals(LocalDate.of(2024, 3, 18), weeks[0].start)
        assertEquals(LocalDate.of(2024, 3, 4), weeks[2].start)
        // Days after today and before the range start are blank.
        assertNull(weeks[0].cells[3])
        assertNull(weeks[2].cells[0])
        assertEquals(2, weeks[2].cells[4]?.count) // Friday 8 March
        assertEquals(1, weeks[0].cells[1]?.count) // Tuesday 19 March
        assertEquals(0, weeks[0].cells[0]?.count)
    }

    @Test
    fun `range starts at the earliest reset when older than creation`() {
        val today = LocalDate.of(2024, 3, 20)
        val weeks = Heatmap.build(listOf(ms(LocalDate.of(2024, 2, 1))), ms(LocalDate.of(2024, 3, 19)), today, DayOfWeek.SUNDAY, zone)
        assertEquals(LocalDate.of(2024, 1, 28), weeks.last().start)
        assertEquals(1, weeks.last().cells[4]?.count)
    }

    @Test
    fun `month labels appear when the month changes and on the oldest row`() {
        val today = LocalDate.of(2024, 3, 20)
        val weeks = Heatmap.build(emptyList(), ms(LocalDate.of(2024, 2, 20)), today, DayOfWeek.MONDAY, zone)
        val labelled = weeks.filter { it.monthLabel != null }.map { it.start }
        assertEquals(listOf(LocalDate.of(2024, 3, 4), LocalDate.of(2024, 2, 19)), labelled)
    }

    @Test
    fun `cell colours`() {
        val empty = 0xFFF3F3F3.toInt(); val one = 0xFFD6D6D6.toInt(); val full = 0xFF000000.toInt()
        assertEquals(empty, Heatmap.cellColor(0, 5, empty, one, full))
        assertEquals(full, Heatmap.cellColor(1, 1, empty, one, full))
        assertEquals(full, Heatmap.cellColor(4, 4, empty, one, full))
        assertEquals(0xFF6B6B6B.toInt(), Heatmap.cellColor(2, 4, empty, one, full))
        assertEquals(listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), Heatmap.weekdays(DayOfWeek.SUNDAY).take(2))
    }
}
