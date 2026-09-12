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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** User-selectable date format. */
enum class DateFormatPreference(val key: String) {
    SYSTEM("system"),
    DAY_MONTH_YEAR("dd/MM/yyyy"),
    MONTH_DAY_YEAR("MM/dd/yyyy"),
    ISO("yyyy-MM-dd");

    companion object {
        fun fromKey(key: String?): DateFormatPreference = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

object DateFormats {
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatDate(
        millis: Long,
        preference: DateFormatPreference,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
        val formatter = when (preference) {
            DateFormatPreference.SYSTEM -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            DateFormatPreference.DAY_MONTH_YEAR -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
            DateFormatPreference.MONTH_DAY_YEAR -> DateTimeFormatter.ofPattern("MM/dd/yyyy")
            DateFormatPreference.ISO -> DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }
        return formatter.format(dateTime)
    }

    fun formatTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        TIME.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun formatDateTime(
        millis: Long,
        preference: DateFormatPreference,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = formatDate(millis, preference, locale, zone) + " " + formatTime(millis, zone)
}
