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

/**
 * The seven display units of an elapsed duration. Declaration order is the
 * canonical display order (largest first) and is also the order in which a
 * unit set is persisted.
 */
enum class DurationUnit(val key: String) {
    YEARS("years"),
    MONTHS("months"),
    WEEKS("weeks"),
    DAYS("days"),
    HOURS("hours"),
    MINUTES("minutes"),
    SECONDS("seconds");

    companion object {
        /** Global default: days, hours, minutes and seconds. */
        val DEFAULT: Set<DurationUnit> = setOf(DAYS, HOURS, MINUTES, SECONDS)

        fun fromKey(key: String): DurationUnit? = entries.firstOrNull { it.key == key }

        /** Parses a list of keys; unknown keys are ignored. Empty result falls back to [DEFAULT]. */
        fun parse(keys: Collection<String>?): Set<DurationUnit> {
            if (keys == null) return DEFAULT
            val set = keys.mapNotNull(::fromKey).toSortedSet()
            return if (set.isEmpty()) DEFAULT else set
        }

        /** Canonical, order-normalised key list for persistence. */
        fun toKeys(units: Set<DurationUnit>): List<String> = units.sorted().map { it.key }
    }
}
