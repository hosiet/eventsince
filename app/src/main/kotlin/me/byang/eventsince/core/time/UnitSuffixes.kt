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

/** Localised short suffixes appended to each unit ("3d", "5min", ...). */
data class UnitSuffixes(
    val years: String,
    val months: String,
    val weeks: String,
    val days: String,
    val hours: String,
    val minutes: String,
    val seconds: String,
) {
    fun of(unit: DurationUnit): String = when (unit) {
        DurationUnit.YEARS -> years
        DurationUnit.MONTHS -> months
        DurationUnit.WEEKS -> weeks
        DurationUnit.DAYS -> days
        DurationUnit.HOURS -> hours
        DurationUnit.MINUTES -> minutes
        DurationUnit.SECONDS -> seconds
    }

    companion object {
        val ENGLISH = UnitSuffixes("y", "m", "w", "d", "h", "min", "s")
    }
}
