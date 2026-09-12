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

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Formats the time elapsed between two instants as a compact string such as
 * `3d 16:35:11`, `2y 5m 11d` or `27:05`, according to a chosen set of units.
 *
 * The algorithm works in two steps:
 *  1. a calendar-field difference with borrowing, computed in [zone];
 *  2. folding the fields into the requested units and rendering them.
 *
 * Known approximation (kept on purpose): when MONTHS is not among the units,
 * the day count switches to absolute days (`floor(ms / 86_400_000)`), and when
 * YEARS is present that value is additionally taken modulo 365.2425.
 */
object ElapsedFormatter {

    const val MILLIS_PER_DAY = 86_400_000L
    const val DAYS_PER_YEAR = 365.2425

    /** Calendar difference between two instants, all fields non-negative. */
    data class CalendarDiff(
        val years: Int,
        val months: Int,
        val days: Int,
        val hours: Int,
        val minutes: Int,
        val seconds: Int,
    )

    fun calendarDiff(startMillis: Long, endMillis: Long, zone: ZoneId): CalendarDiff {
        val start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startMillis), zone)
        val end = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endMillis), zone)

        var seconds = end.second - start.second
        var borrow = 0
        if (seconds < 0) { seconds += 60; borrow = 1 }

        var minutes = end.minute - start.minute - borrow
        borrow = 0
        if (minutes < 0) { minutes += 60; borrow = 1 }

        var hours = end.hour - start.hour - borrow
        borrow = 0
        if (hours < 0) { hours += 24; borrow = 1 }

        var days = end.dayOfMonth - start.dayOfMonth - borrow
        borrow = 0
        if (days < 0) {
            days += YearMonth.of(start.year, start.month).lengthOfMonth()
            borrow = 1
        }

        var months = end.monthValue - start.monthValue - borrow
        borrow = 0
        if (months < 0) { months += 12; borrow = 1 }

        val years = end.year - start.year - borrow
        return CalendarDiff(years, months, days, hours, minutes, seconds)
    }

    /**
     * @param startMillis start of the period
     * @param endMillis end of the period (usually "now", or the stop time)
     * @param units units to display; must not be empty
     */
    fun format(
        startMillis: Long,
        endMillis: Long,
        units: Set<DurationUnit>,
        suffixes: UnitSuffixes = UnitSuffixes.ENGLISH,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        require(units.isNotEmpty()) { "units must not be empty" }
        val end = if (endMillis < startMillis) startMillis else endMillis
        val diff = calendarDiff(startMillis, end, zone)
        val totalMillis = end - startMillis

        var years = diff.years.toLong()
        var months = diff.months.toLong()
        var weeks = 0L
        var days = diff.days.toLong()
        var hours = diff.hours.toLong()
        var minutes = diff.minutes.toLong()
        var seconds = diff.seconds.toLong()

        fun hasSmaller(unit: DurationUnit) = units.any { it.ordinal > unit.ordinal }

        val out = StringBuilder()
        fun emit(token: String) {
            out.append(token)
            if (!token.endsWith(':')) out.append(' ')
        }

        if (DurationUnit.YEARS !in units) {
            months += years * 12
        } else if (years > 0 || !hasSmaller(DurationUnit.YEARS)) {
            emit("$years${suffixes.years}")
        }

        if (DurationUnit.MONTHS !in units) {
            val absoluteDays = totalMillis / MILLIS_PER_DAY
            days = if (DurationUnit.YEARS in units) {
                Math.floor(absoluteDays % DAYS_PER_YEAR).toLong()
            } else {
                absoluteDays
            }
        } else if (months > 0 || out.isNotEmpty() || !hasSmaller(DurationUnit.MONTHS)) {
            emit("$months${suffixes.months}")
        }

        if (DurationUnit.WEEKS in units) {
            weeks = days / 7
            days %= 7
            if (weeks > 0 || out.isNotEmpty() || !hasSmaller(DurationUnit.WEEKS)) {
                emit("$weeks${suffixes.weeks}")
            }
        }

        if (DurationUnit.DAYS !in units) {
            hours += days * 24
        } else if (days > 0 || out.isNotEmpty() || !hasSmaller(DurationUnit.DAYS)) {
            emit("$days${suffixes.days}")
        }

        val hasHours = DurationUnit.HOURS in units
        val hasMinutes = DurationUnit.MINUTES in units
        val hasSeconds = DurationUnit.SECONDS in units

        if (!hasHours) {
            minutes += hours * 60
        } else if (!hasMinutes) {
            if (hours > 0 || out.isNotEmpty() || !hasSmaller(DurationUnit.HOURS)) {
                emit("$hours${suffixes.hours}")
            }
        } else {
            emit(twoDigits(hours) + ":")
        }

        if (!hasMinutes) {
            seconds += minutes * 60
        } else if (!hasHours) {
            if (minutes > 0 || out.isNotEmpty() || !hasSeconds) {
                emit("$minutes${suffixes.minutes}")
            }
        } else {
            emit(twoDigits(minutes) + if (hasSeconds) ":" else "")
        }

        if (hasSeconds) {
            if (hasHours && hasMinutes) emit(twoDigits(seconds)) else emit("$seconds${suffixes.seconds}")
        }

        return out.toString().trimEnd()
    }

    /** How often a running display using [units] needs to be refreshed. */
    fun refreshIntervalMillis(units: Set<DurationUnit>): Long =
        if (DurationUnit.SECONDS in units) 1_000L else 60_000L

    private fun twoDigits(value: Long): String = if (value < 10) "0$value" else value.toString()
}
