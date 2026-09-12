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
package me.byang.eventsince.data.local

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.LogType
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.domain.model.SortOrder

private val json = Json { ignoreUnknownKeys = true }
private val stringList = ListSerializer(String.serializer())

object FormatCodec {
    fun encode(units: Set<DurationUnit>?): String? =
        units?.let { json.encodeToString(stringList, DurationUnit.toKeys(it)) }

    fun decode(raw: String?): Set<DurationUnit>? {
        if (raw.isNullOrBlank()) return null
        val keys = runCatching { json.decodeFromString(stringList, raw) }.getOrNull() ?: return null
        val units = keys.mapNotNull(DurationUnit::fromKey).toSortedSet()
        return units.ifEmpty { null }
    }
}

fun CategoryEntity.toDomain() = Category(id, name, position, SortOrder.fromKey(sort), isDefault)
fun Category.toEntity() = CategoryEntity(id, name, position, sort.key, isDefault)

fun EventEntity.toDomain() = Event(
    id = id,
    categoryId = categoryId,
    label = label,
    startAt = startAt,
    stoppedAt = stoppedAt,
    colorIndex = colorIndex,
    createdAt = createdAt,
    format = FormatCodec.decode(format),
    position = position,
    archivedAt = archivedAt,
)

fun Event.toEntity() = EventEntity(
    id = id,
    categoryId = categoryId,
    label = label,
    startAt = startAt,
    stoppedAt = stoppedAt,
    colorIndex = colorIndex,
    createdAt = createdAt,
    format = FormatCodec.encode(format),
    position = position,
    archivedAt = archivedAt,
)

fun EventLogEntity.toDomain() = EventLog(
    id = id,
    eventId = eventId,
    type = LogType.fromKey(type) ?: LogType.EDITED,
    at = at,
    previousStartAt = previousStartAt,
    newStartAt = newStartAt,
    elapsedMs = elapsedMs,
    labelSnapshot = labelSnapshot,
    colorHex = colorHex,
    notes = notes,
    payload = payload,
)

fun EventLog.toEntity() = EventLogEntity(
    id = id,
    eventId = eventId,
    type = type.name,
    at = at,
    previousStartAt = previousStartAt,
    newStartAt = newStartAt,
    elapsedMs = elapsedMs,
    labelSnapshot = labelSnapshot,
    colorHex = colorHex,
    notes = notes,
    payload = payload,
)

fun ReminderEntity.toDomain() = Reminder(id, eventId, target, ReminderUnit.fromKey(unit), body, createdAt)
fun Reminder.toEntity() = ReminderEntity(id, eventId, target, unit.key, body, createdAt)
