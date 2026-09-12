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
package me.byang.eventsince.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import me.byang.eventsince.core.color.Palettes
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.prefs.CategoryHeaderMode
import me.byang.eventsince.data.prefs.DarkMode
import me.byang.eventsince.data.prefs.UserPreferences
import me.byang.eventsince.data.prefs.WeekStart
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.DataSnapshot
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.LogType
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.domain.model.SortOrder

/** EventSince's own backup file. Bump [CURRENT_SCHEMA] when the structure changes. */
@Serializable
data class BackupFile(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val exportedAt: Long,
    val app: AppInfo,
    val preferences: PreferencesDto? = null,
    val categories: List<CategoryDto>,
) {
    companion object {
        const val CURRENT_SCHEMA = 2
    }
}

@Serializable
data class AppInfo(val name: String, val version: String, val versionCode: Int = 0)

@Serializable
data class PreferencesDto(
    val darkMode: String,
    val dynamicColor: Boolean = true,
    val palette: String,
    val format: List<String>,
    val dateFormat: String,
    val weekStart: String,
    val categoryHeaders: String,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String = "",
    val position: Int = 0,
    val sort: String = "ASC",
    val isDefault: Boolean = false,
    val events: List<EventDto> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EventDto(
    val id: String,
    val label: String = "",
    val startAt: Long,
    val stoppedAt: Long? = null,
    val colorIndex: Int = 0,
    val createdAt: Long,
    val format: List<String>? = null,
    val position: Int = 0,
    /** Schema 1 wrote this as `deletedAt`. */
    @JsonNames("deletedAt") val archivedAt: Long? = null,
    val logs: List<LogDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
)

@Serializable
data class LogDto(
    val id: String,
    val type: String,
    val at: Long,
    val previousStartAt: Long? = null,
    val newStartAt: Long? = null,
    val elapsedMs: Long? = null,
    val labelSnapshot: String? = null,
    val colorHex: String? = null,
    val notes: String? = null,
    val payload: String? = null,
)

@Serializable
data class ReminderDto(
    val id: String,
    val target: Int,
    val unit: String = "days",
    val body: String? = null,
    val createdAt: Long = 0,
)

// ---------------------------------------------------------------- mapping

fun UserPreferences.toDto() = PreferencesDto(
    darkMode = darkMode.key,
    dynamicColor = dynamicColor,
    palette = paletteKey,
    format = DurationUnit.toKeys(format),
    dateFormat = dateFormat.key,
    weekStart = weekStart.key,
    categoryHeaders = categoryHeaders.key,
)

fun PreferencesDto.toPreferences() = UserPreferences(
    darkMode = DarkMode.fromKey(darkMode),
    dynamicColor = dynamicColor,
    paletteKey = if (palette == Palettes.SYSTEM_KEY || Palettes.byKey(palette) != null) palette else Palettes.DEFAULT_KEY,
    format = DurationUnit.parse(format),
    dateFormat = DateFormatPreference.fromKey(dateFormat),
    weekStart = WeekStart.fromKey(weekStart),
    categoryHeaders = CategoryHeaderMode.fromKey(categoryHeaders),
)

fun DataSnapshot.toDtos(): List<CategoryDto> {
    val logsByEvent = logs.groupBy { it.eventId }
    val remindersByEvent = reminders.groupBy { it.eventId }
    val eventsByCategory = events.groupBy { it.categoryId }
    return categories.sortedBy { it.position }.map { c ->
        CategoryDto(
            id = c.id,
            name = c.name,
            position = c.position,
            sort = c.sort.key,
            isDefault = c.isDefault,
            events = eventsByCategory[c.id].orEmpty().sortedBy { it.position }.map { e ->
                EventDto(
                    id = e.id,
                    label = e.label,
                    startAt = e.startAt,
                    stoppedAt = e.stoppedAt,
                    colorIndex = e.colorIndex,
                    createdAt = e.createdAt,
                    format = e.format?.let(DurationUnit::toKeys),
                    position = e.position,
                    archivedAt = e.archivedAt,
                    logs = logsByEvent[e.id].orEmpty().sortedBy { it.at }.map { l ->
                        LogDto(l.id, l.type.name, l.at, l.previousStartAt, l.newStartAt, l.elapsedMs, l.labelSnapshot, l.colorHex, l.notes, l.payload)
                    },
                    reminders = remindersByEvent[e.id].orEmpty().map { r -> ReminderDto(r.id, r.target, r.unit.key, r.body, r.createdAt) },
                )
            },
        )
    }
}

fun List<CategoryDto>.toSnapshot(): DataSnapshot {
    val categories = mutableListOf<Category>()
    val events = mutableListOf<Event>()
    val logs = mutableListOf<EventLog>()
    val reminders = mutableListOf<Reminder>()
    forEachIndexed { ci, c ->
        categories += Category(c.id, c.name, ci, SortOrder.fromKey(c.sort), c.isDefault)
        c.events.forEachIndexed { ei, e ->
            events += Event(
                id = e.id,
                categoryId = c.id,
                label = e.label,
                startAt = e.startAt,
                stoppedAt = e.stoppedAt?.takeIf { it > 0 },
                colorIndex = e.colorIndex.coerceIn(0, 14),
                createdAt = e.createdAt,
                format = e.format?.let { DurationUnit.parse(it) }?.takeIf { e.format.isNotEmpty() },
                position = ei,
                archivedAt = e.archivedAt,
            )
            e.logs.forEach { l ->
                val type = LogType.fromKey(l.type) ?: return@forEach
                logs += EventLog(l.id, e.id, type, l.at, l.previousStartAt, l.newStartAt, l.elapsedMs, l.labelSnapshot, l.colorHex, l.notes, l.payload)
            }
            e.reminders.forEach { r ->
                if (r.target > 0) reminders += Reminder(r.id, e.id, r.target, ReminderUnit.fromKey(r.unit), r.body, r.createdAt)
            }
        }
    }
    if (categories.isNotEmpty() && categories.none { it.isDefault }) categories[0] = categories[0].copy(isDefault = true)
    return DataSnapshot(categories, events, logs, reminders)
}
