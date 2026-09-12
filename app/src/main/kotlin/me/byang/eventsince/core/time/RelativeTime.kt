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

/** "N ago" style distance between an instant and now, in days / hours / minutes. */
object RelativeTime {

    data class Parts(val days: Long, val hours: Long, val minutes: Long) {
        val isNow: Boolean get() = days == 0L && hours == 0L && minutes == 0L
    }

    fun parts(nowMillis: Long, thenMillis: Long): Parts {
        val diff = (nowMillis - thenMillis).coerceAtLeast(0)
        val days = diff / 86_400_000L
        val hours = diff % 86_400_000L / 3_600_000L
        val minutes = diff % 3_600_000L / 60_000L
        return Parts(days, hours, minutes)
    }

    /**
     * Returns the compact distance string (e.g. `3d 4h 12min`) or `null` when the
     * distance is below one minute, in which case the caller shows "just now".
     */
    fun format(nowMillis: Long, thenMillis: Long, suffixes: UnitSuffixes = UnitSuffixes.ENGLISH): String? {
        val p = parts(nowMillis, thenMillis)
        if (p.isNow) return null
        val tokens = buildList {
            if (p.days > 0) add("${p.days}${suffixes.days}")
            if (p.hours != 0L) add("${p.hours}${suffixes.hours}")
            if (p.minutes != 0L) add("${p.minutes}${suffixes.minutes}")
        }
        return tokens.joinToString(" ")
    }
}
