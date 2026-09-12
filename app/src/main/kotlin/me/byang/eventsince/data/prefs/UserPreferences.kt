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
package me.byang.eventsince.data.prefs

import me.byang.eventsince.core.color.Palettes
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.core.time.DurationUnit

enum class DarkMode(val key: String) {
    LIGHT("light"), DARK("dark"), SYSTEM("auto");

    companion object {
        fun fromKey(key: String?): DarkMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class WeekStart(val key: String) {
    SYSTEM("system"), SUNDAY("sunday"), MONDAY("monday");

    companion object {
        fun fromKey(key: String?): WeekStart = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class CategoryHeaderMode(val key: String) {
    AUTO("auto"), ALWAYS("always");

    companion object {
        fun fromKey(key: String?): CategoryHeaderMode = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

data class UserPreferences(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val paletteKey: String = Palettes.DEFAULT_KEY,
    val format: Set<DurationUnit> = DurationUnit.DEFAULT,
    val dateFormat: DateFormatPreference = DateFormatPreference.SYSTEM,
    val weekStart: WeekStart = WeekStart.SYSTEM,
    val categoryHeaders: CategoryHeaderMode = CategoryHeaderMode.AUTO,
    val seeded: Boolean = false,
)
