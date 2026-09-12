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
package me.byang.eventsince.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.local.EventSinceDatabase
import me.byang.eventsince.data.local.FormatCodec
import me.byang.eventsince.data.local.toDomain
import me.byang.eventsince.data.local.toEntity
import me.byang.eventsince.domain.ResetSemantics
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.CategoryDeletion
import me.byang.eventsince.domain.model.CategoryWithEvents
import me.byang.eventsince.domain.model.DataSnapshot
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.EventPlacement
import me.byang.eventsince.domain.model.LogType
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.domain.model.SortOrder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEventRepository @Inject constructor(
    private val db: EventSinceDatabase,
) : EventRepository {

    private val categories get() = db.categoryDao()
    private val events get() = db.eventDao()
    private val logs get() = db.eventLogDao()
    private val reminders get() = db.reminderDao()

    private fun newId(): String = UUID.randomUUID().toString()

    // ---------------- categories ----------------

    override fun observeCategories(): Flow<List<Category>> =
        categories.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getCategories(): List<Category> = categories.getAll().map { it.toDomain() }

    override suspend fun getCategory(id: String): Category? = categories.getById(id)?.toDomain()

    override suspend fun getDefaultCategory(): Category = db.withTransaction {
        categories.getDefault()?.toDomain() ?: run {
            val first = categories.getAll().firstOrNull()
            if (first != null) {
                categories.update(first.copy(isDefault = true))
                first.copy(isDefault = true).toDomain()
            } else {
                val created = Category(newId(), "", 0, SortOrder.ASC, isDefault = true)
                categories.insert(created.toEntity())
                created
            }
        }
    }

    override suspend fun createCategory(name: String): Category = db.withTransaction {
        val category = Category(newId(), name.trim(), categories.maxPosition() + 1, SortOrder.ASC, isDefault = false)
        categories.insert(category.toEntity())
        category
    }

    override suspend fun renameCategory(id: String, name: String) {
        val existing = categories.getById(id) ?: return
        categories.update(existing.copy(name = name.trim()))
    }

    override suspend fun reorderCategories(orderedIds: List<String>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id -> categories.updatePosition(id, index) }
    }

    override suspend fun toggleCategorySort(id: String): SortOrder = db.withTransaction {
        val category = requireNotNull(categories.getById(id)) { "category $id" }
        val newSort = SortOrder.fromKey(category.sort).flipped()
        val sorted = events.getActiveInCategory(id).sortedBy { it.startAt }
            .let { if (newSort == SortOrder.DESC) it.reversed() else it }
        sorted.forEachIndexed { index, e -> events.updatePlacement(e.id, id, index) }
        categories.updateSort(id, newSort.key)
        newSort
    }

    override suspend fun deleteCategory(id: String, deletion: CategoryDeletion, now: Long): List<String> =
        db.withTransaction {
            val category = categories.getById(id) ?: return@withTransaction emptyList()
            require(!category.isDefault) { "the default category cannot be deleted" }
            val default = getDefaultCategory()
            val affected = events.getActiveInCategory(id)
            when (deletion) {
                is CategoryDeletion.DeleteEvents -> events.deleteActiveInCategory(id)
                is CategoryDeletion.MoveEvents -> {
                    val targetId = deletion.targetCategoryId?.takeIf { it != id } ?: default.id
                    val base = events.maxPositionInCategory(targetId) + 1
                    affected.forEachIndexed { index, e -> events.updatePlacement(e.id, targetId, base + index) }
                }
            }
            // Archived events of this category keep existing; they need a valid category.
            events.moveAll(id, default.id)
            categories.deleteById(id)
            categories.getAll().forEachIndexed { index, c -> if (c.position != index) categories.updatePosition(c.id, index) }
            if (deletion is CategoryDeletion.DeleteEvents) affected.map { it.id } else emptyList()
        }

    // ---------------- events ----------------

    override fun observeHome(): Flow<List<CategoryWithEvents>> =
        combine(categories.observeAll(), events.observeActive()) { cats, evs ->
            val byCategory = evs.groupBy { it.categoryId }
            cats.map { c ->
                CategoryWithEvents(c.toDomain(), byCategory[c.id].orEmpty().sortedBy { it.position }.map { it.toDomain() })
            }
        }

    override fun observeArchived(): Flow<List<Event>> =
        events.observeArchived().map { list -> list.map { it.toDomain() } }

    override fun observeEvent(id: String): Flow<Event?> = events.observeById(id).map { it?.toDomain() }

    override suspend fun getEvent(id: String): Event? = events.getById(id)?.toDomain()

    override suspend fun getActiveEvents(): List<Event> = events.getActive().map { it.toDomain() }

    override suspend fun createEvent(label: String, colorIndex: Int, categoryId: String?, startAt: Long, now: Long): Event =
        db.withTransaction {
            val category = categoryId?.let { categories.getById(it) }?.toDomain() ?: getDefaultCategory()
            val event = Event(
                id = newId(),
                categoryId = category.id,
                label = label.trim(),
                startAt = startAt,
                stoppedAt = null,
                colorIndex = colorIndex.coerceIn(0, 14),
                createdAt = startAt,
                format = null,
                position = events.maxPositionInCategory(category.id) + 1,
            )
            events.insert(event.toEntity())
            logs.insert(EventLog(newId(), event.id, LogType.CREATED, at = now, newStartAt = startAt).toEntity())
            event
        }

    override suspend fun resetEvent(id: String, requestedStartAt: Long?, colorHex: String, now: Long): Event? =
        db.withTransaction {
            val event = events.getById(id)?.toDomain() ?: return@withTransaction null
            val result = ResetSemantics.reset(event, requestedStartAt, now)
            val updated = event.copy(startAt = result.newStartAt, stoppedAt = null)
            events.update(updated.toEntity())
            logs.insert(
                EventLog(
                    id = newId(),
                    eventId = id,
                    type = LogType.RESET,
                    at = now,
                    previousStartAt = result.previousStartAt,
                    newStartAt = result.newStartAt,
                    elapsedMs = result.elapsedMs,
                    labelSnapshot = event.label,
                    colorHex = colorHex,
                    notes = null,
                ).toEntity(),
            )
            updated
        }

    override suspend fun stopEvent(id: String, now: Long): Event? = db.withTransaction {
        val event = events.getById(id)?.toDomain() ?: return@withTransaction null
        if (!event.isRunning) return@withTransaction event
        val updated = event.copy(stoppedAt = now)
        events.update(updated.toEntity())
        logs.insert(
            EventLog(
                id = newId(),
                eventId = id,
                type = LogType.STOPPED,
                at = now,
                previousStartAt = event.startAt,
                elapsedMs = ResetSemantics.stopElapsed(event, now),
            ).toEntity(),
        )
        updated
    }

    override suspend fun editEvent(id: String, label: String, colorIndex: Int, categoryId: String, now: Long): Event? =
        db.withTransaction {
            val event = events.getById(id)?.toDomain() ?: return@withTransaction null
            val newLabel = label.trim()
            val newColor = colorIndex.coerceIn(0, 14)
            val newCategoryId = categories.getById(categoryId)?.id ?: event.categoryId
            val changes = buildJsonObject {
                if (newLabel != event.label) put("label", change(JsonPrimitive(event.label), JsonPrimitive(newLabel)))
                if (newColor != event.colorIndex) put("color", change(JsonPrimitive(event.colorIndex), JsonPrimitive(newColor)))
                if (newCategoryId != event.categoryId) put("category", change(JsonPrimitive(event.categoryId), JsonPrimitive(newCategoryId)))
            }
            if (changes.isEmpty()) return@withTransaction event
            val position = if (newCategoryId != event.categoryId) events.maxPositionInCategory(newCategoryId) + 1 else event.position
            val updated = event.copy(label = newLabel, colorIndex = newColor, categoryId = newCategoryId, position = position)
            events.update(updated.toEntity())
            logs.insert(EventLog(newId(), id, LogType.EDITED, at = now, payload = changes.toString()).toEntity())
            updated
        }

    override suspend fun setEventFormat(id: String, format: Set<DurationUnit>?, now: Long) = db.withTransaction {
        val event = events.getById(id)?.toDomain() ?: return@withTransaction
        if (event.format == format) return@withTransaction
        events.update(event.copy(format = format).toEntity())
        val changes = buildJsonObject { put("format", change(unitsJson(event.format), unitsJson(format))) }
        logs.insert(EventLog(newId(), id, LogType.EDITED, at = now, payload = changes.toString()).toEntity())
    }

    override suspend fun applyFormatToAllEvents(format: Set<DurationUnit>) {
        events.setFormatForAll(FormatCodec.encode(format))
    }

    override suspend fun archiveEvent(id: String, now: Long) = db.withTransaction {
        val event = events.getById(id) ?: return@withTransaction
        if (event.archivedAt != null) return@withTransaction
        events.setArchivedAt(id, now)
        logs.insert(EventLog(newId(), id, LogType.ARCHIVED, at = now).toEntity())
    }

    override suspend fun unarchiveEvent(id: String, now: Long) = db.withTransaction {
        val event = events.getById(id) ?: return@withTransaction
        if (event.archivedAt == null) return@withTransaction
        events.setArchivedAt(id, null)
        logs.insert(EventLog(newId(), id, LogType.UNARCHIVED, at = now).toEntity())
    }

    override suspend fun deleteEvent(id: String) = events.deleteById(id)

    override suspend fun applyHomeLayout(categoryOrder: List<String>, placements: List<EventPlacement>) =
        db.withTransaction {
            categoryOrder.forEachIndexed { index, id -> categories.updatePosition(id, index) }
            placements.forEach { events.updatePlacement(it.eventId, it.categoryId, it.position) }
        }

    // ---------------- logs ----------------

    override fun observeLogs(eventId: String): Flow<List<EventLog>> =
        logs.observeForEvent(eventId).map { list -> list.map { it.toDomain() } }

    override suspend fun getLogs(eventId: String): List<EventLog> = logs.getForEvent(eventId).map { it.toDomain() }

    override suspend fun updateLogNotes(logId: String, notes: String?) = logs.updateNotes(logId, notes?.ifBlank { null })

    override suspend fun deleteLog(logId: String) = logs.deleteById(logId)

    override suspend fun clearResetLogs(eventId: String) = logs.deleteByType(eventId, LogType.RESET.name)

    // ---------------- reminders ----------------

    override fun observeReminders(eventId: String): Flow<List<Reminder>> =
        reminders.observeForEvent(eventId).map { list -> list.map { it.toDomain() } }

    override suspend fun getReminders(eventId: String): List<Reminder> =
        reminders.getForEvent(eventId).map { it.toDomain() }

    override suspend fun getAllReminders(): List<Reminder> = reminders.getAll().map { it.toDomain() }

    override suspend fun countReminders(): Int = reminders.count()

    override suspend fun addReminder(eventId: String, target: Int, unit: ReminderUnit, now: Long): Reminder {
        val reminder = Reminder(newId(), eventId, target, unit, body = null, createdAt = now)
        reminders.insert(reminder.toEntity())
        return reminder
    }

    override suspend fun getReminder(id: String): Reminder? = reminders.getById(id)?.toDomain()

    override suspend fun deleteReminder(id: String) = reminders.deleteById(id)

    // ---------------- bulk ----------------

    override suspend fun snapshot(): DataSnapshot = db.withTransaction {
        DataSnapshot(
            categories = categories.getAll().map { it.toDomain() },
            events = events.getAllIncludingArchived().map { it.toDomain() },
            logs = logs.getAll().map { it.toDomain() },
            reminders = reminders.getAll().map { it.toDomain() },
        )
    }

    override suspend fun replaceAll(snapshot: DataSnapshot) = db.withTransaction {
        reminders.deleteAll()
        logs.deleteAll()
        events.deleteAll()
        categories.deleteAll()
        insertSnapshot(snapshot, positionOffset = 0)
    }

    override suspend fun appendAll(snapshot: DataSnapshot) = db.withTransaction {
        insertSnapshot(snapshot, positionOffset = categories.maxPosition() + 1)
    }

    private suspend fun insertSnapshot(snapshot: DataSnapshot, positionOffset: Int) {
        val hasDefault = positionOffset > 0 || snapshot.categories.any { it.isDefault }
        val cats = snapshot.categories.mapIndexed { index, c ->
            c.copy(position = positionOffset + index, isDefault = if (positionOffset > 0) false else c.isDefault || (!hasDefault && index == 0))
        }
        categories.insertAll(cats.map { it.toEntity() })
        val validCategoryIds = cats.map { it.id }.toSet()
        val fallback = cats.firstOrNull()?.id ?: getDefaultCategory().id
        events.insertAll(snapshot.events.map { e ->
            (if (e.categoryId in validCategoryIds) e else e.copy(categoryId = fallback)).toEntity()
        })
        val eventIds = snapshot.events.map { it.id }.toSet()
        logs.insertAll(snapshot.logs.filter { it.eventId in eventIds }.map { it.toEntity() })
        reminders.insertAll(snapshot.reminders.filter { it.eventId in eventIds }.map { it.toEntity() })
    }

    // ---------------- helpers ----------------

    private fun change(from: kotlinx.serialization.json.JsonElement, to: kotlinx.serialization.json.JsonElement): JsonObject =
        buildJsonObject { put("from", from); put("to", to) }

    private fun unitsJson(units: Set<DurationUnit>?): kotlinx.serialization.json.JsonElement =
        if (units == null) kotlinx.serialization.json.JsonNull
        else buildJsonArray { DurationUnit.toKeys(units).forEach { add(JsonPrimitive(it)) } }
}
