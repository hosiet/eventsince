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

import me.byang.eventsince.core.time.DurationUnit.DAYS
import me.byang.eventsince.core.time.DurationUnit.HOURS
import me.byang.eventsince.core.time.DurationUnit.MINUTES
import me.byang.eventsince.core.time.DurationUnit.MONTHS
import me.byang.eventsince.core.time.DurationUnit.SECONDS
import me.byang.eventsince.core.time.DurationUnit.WEEKS
import me.byang.eventsince.core.time.DurationUnit.YEARS
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test
import kotlin.test.assertEquals

class ElapsedFormatterTest {
    private val zone: ZoneId = ZoneId.of("UTC")

    private fun ms(y: Int, mo: Int, d: Int, h: Int = 0, mi: Int = 0, s: Int = 0): Long =
        LocalDateTime.of(y, mo, d, h, mi, s).atZone(zone).toInstant().toEpochMilli()

    private fun fmt(start: Long, end: Long, vararg units: DurationUnit) =
        ElapsedFormatter.format(start, end, units.toSet(), UnitSuffixes.ENGLISH, zone)

    private val default = arrayOf(DAYS, HOURS, MINUTES, SECONDS)

    @Test
    fun `default format with days`() {
        val start = ms(2024, 1, 1, 10, 0, 0)
        val end = ms(2024, 1, 5, 2, 35, 11) // 3 days 16:35:11
        assertEquals("3d 16:35:11", fmt(start, end, *default))
    }

    @Test
    fun `default format below one day omits days`() {
        val start = ms(2024, 1, 1)
        assertEquals("00:00:05", fmt(start, start + 5_000, *default))
        assertEquals("00:00:00", fmt(start, start, *default))
    }

    @Test
    fun `years months days`() {
        val start = ms(2021, 3, 4)
        assertEquals("2y 5m 11d", fmt(start, ms(2023, 8, 15), YEARS, MONTHS, DAYS))
        assertEquals("5m 11d", fmt(start, ms(2021, 8, 15), YEARS, MONTHS, DAYS))
        assertEquals("0d", fmt(start, start + 3_600_000, YEARS, MONTHS, DAYS))
    }

    @Test
    fun `months borrow days from start month`() {
        // 31 Jan -> 1 Mar 2023: day diff negative, borrow the 31 days of January.
        assertEquals("1m 1d", fmt(ms(2023, 1, 31), ms(2023, 3, 1), YEARS, MONTHS, DAYS))
        // Leap-year February: 29 days are borrowed.
        assertEquals("1m 0d", fmt(ms(2024, 2, 1), ms(2024, 3, 1), MONTHS, DAYS))
        assertEquals("28d", fmt(ms(2024, 2, 1), ms(2024, 2, 29, 23), MONTHS, DAYS))
        assertEquals("1m 25d", fmt(ms(2024, 2, 5), ms(2024, 4, 1), MONTHS, DAYS))
    }

    @Test
    fun `days only`() {
        val start = ms(2024, 1, 1, 12)
        assertEquals("3d", fmt(start, ms(2024, 1, 4, 20), DAYS))
        assertEquals("0d", fmt(start, start + 60_000, DAYS))
    }

    @Test
    fun `hours and minutes fold days into hours`() {
        val start = ms(2024, 1, 1, 0, 0)
        assertEquals("27:05", fmt(start, ms(2024, 1, 2, 3, 5, 59), HOURS, MINUTES))
        assertEquals("00:00", fmt(start, start, HOURS, MINUTES))
    }

    @Test
    fun `weeks days hours minutes seconds`() {
        val start = ms(2024, 1, 1)
        assertEquals("1w 3d 01:00:00", fmt(start, ms(2024, 1, 11, 1), WEEKS, DAYS, HOURS, MINUTES, SECONDS))
        assertEquals("2d 00:00:00", fmt(start, ms(2024, 1, 3), WEEKS, DAYS, HOURS, MINUTES, SECONDS))
        assertEquals("00:00:10", fmt(start, start + 10_000, WEEKS, DAYS, HOURS, MINUTES, SECONDS))
    }

    @Test
    fun `seconds only`() {
        val start = ms(2024, 1, 1)
        assertEquals("93784s", fmt(start, start + 93_784_000, SECONDS))
    }

    @Test
    fun `hours only and minutes only`() {
        val start = ms(2024, 1, 1)
        assertEquals("26h", fmt(start, ms(2024, 1, 2, 2, 30), HOURS))
        assertEquals("0h", fmt(start, start + 1_000, HOURS))
        assertEquals("1590min", fmt(start, ms(2024, 1, 2, 2, 30), MINUTES))
        assertEquals("1590min 7s", fmt(start, ms(2024, 1, 2, 2, 30, 7), MINUTES, SECONDS))
        assertEquals("7s", fmt(start, start + 7_000, MINUTES, SECONDS))
    }

    @Test
    fun `years without months uses absolute days modulo year`() {
        val start = ms(2020, 1, 1)
        val end = ms(2021, 1, 11) // 376 absolute days
        assertEquals("1y 10d", fmt(start, end, YEARS, DAYS))
        // Under a year: absolute days.
        assertEquals("100d", fmt(start, start + 100 * 86_400_000L, YEARS, DAYS))
    }

    @Test
    fun `weeks without months use absolute days`() {
        val start = ms(2024, 1, 1)
        assertEquals("6w 3d", fmt(start, start + 45 * 86_400_000L, WEEKS, DAYS))
        assertEquals("0w", fmt(start, start + 1_000, WEEKS))
    }

    @Test
    fun `end before start is treated as zero`() {
        val start = ms(2024, 1, 1)
        assertEquals("00:00:00", fmt(start, start - 5_000, *default))
    }

    @Test
    fun `suffixes are localised`() {
        val zh = UnitSuffixes("年", "月", "周", "天", "时", "分", "秒")
        val start = ms(2024, 1, 1)
        assertEquals("3天 00:00:00", ElapsedFormatter.format(start, ms(2024, 1, 4), DurationUnit.DEFAULT, zh, zone))
    }

    @Test
    fun `refresh interval`() {
        assertEquals(1_000L, ElapsedFormatter.refreshIntervalMillis(DurationUnit.DEFAULT))
        assertEquals(60_000L, ElapsedFormatter.refreshIntervalMillis(setOf(DAYS, HOURS)))
    }

    @Test
    fun `unit parsing normalises order and ignores junk`() {
        assertEquals(setOf(DAYS, HOURS), DurationUnit.parse(listOf("hours", "bogus", "days")))
        assertEquals(listOf("days", "hours"), DurationUnit.toKeys(setOf(HOURS, DAYS)))
        assertEquals(DurationUnit.DEFAULT, DurationUnit.parse(null))
        assertEquals(DurationUnit.DEFAULT, DurationUnit.parse(emptyList()))
    }
}
