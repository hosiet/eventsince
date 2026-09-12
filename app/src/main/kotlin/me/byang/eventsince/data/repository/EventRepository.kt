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

import kotlinx.coroutines.flow.Flow
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.CategoryDeletion
import me.byang.eventsince.domain.model.CategoryWithEvents
import me.byang.eventsince.domain.model.DataSnapshot
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.EventPlacement
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.domain.model.SortOrder

/**
 * Persistence for categories, events, their operation logs and reminders.
 * Methods are pure data operations: they write the appropriate log entries but
 * do not schedule alarms or trigger backups (see `EventService`).
 */
interface EventRepository {
    // ---- categories ----
    fun observeCategories(): Flow<List<Category>>
    suspend fun getCategories(): List<Category>
    suspend fun getCategory(id: String): Category?
    suspend fun getDefaultCategory(): Category
    suspend fun createCategory(name: String): Category
    suspend fun renameCategory(id: String, name: String)
    suspend fun reorderCategories(orderedIds: List<String>)
    /** Sorts the category's events by start instant in the flipped order and stores the new order. */
    suspend fun toggleCategorySort(id: String): SortOrder
    /** Returns the ids of events that were permanently deleted as part of the deletion. */
    suspend fun deleteCategory(id: String, deletion: CategoryDeletion, now: Long): List<String>

    // ---- events ----
    fun observeHome(): Flow<List<CategoryWithEvents>>
    /** Archived events, most recently archived first. */
    fun observeArchived(): Flow<List<Event>>
    fun observeEvent(id: String): Flow<Event?>
    suspend fun getEvent(id: String): Event?
    suspend fun getActiveEvents(): List<Event>
    suspend fun createEvent(label: String, colorIndex: Int, categoryId: String?, startAt: Long, now: Long): Event
    suspend fun resetEvent(id: String, requestedStartAt: Long?, colorHex: String, now: Long): Event?
    suspend fun stopEvent(id: String, now: Long): Event?
    suspend fun editEvent(id: String, label: String, colorIndex: Int, categoryId: String, now: Long): Event?
    suspend fun setEventFormat(id: String, format: Set<DurationUnit>?, now: Long)
    suspend fun applyFormatToAllEvents(format: Set<DurationUnit>)
    /** Hides the event from the main list; it stays in the database and in backups. */
    suspend fun archiveEvent(id: String, now: Long)
    suspend fun unarchiveEvent(id: String, now: Long)
    /** Removes the event, its logs and its reminders permanently. */
    suspend fun deleteEvent(id: String)
    suspend fun applyHomeLayout(categoryOrder: List<String>, placements: List<EventPlacement>)

    // ---- logs ----
    fun observeLogs(eventId: String): Flow<List<EventLog>>
    suspend fun getLogs(eventId: String): List<EventLog>
    suspend fun updateLogNotes(logId: String, notes: String?)
    suspend fun deleteLog(logId: String)
    suspend fun clearResetLogs(eventId: String)

    // ---- reminders ----
    fun observeReminders(eventId: String): Flow<List<Reminder>>
    suspend fun getReminders(eventId: String): List<Reminder>
    suspend fun getAllReminders(): List<Reminder>
    suspend fun countReminders(): Int
    suspend fun addReminder(eventId: String, target: Int, unit: ReminderUnit, now: Long): Reminder
    suspend fun getReminder(id: String): Reminder?
    suspend fun deleteReminder(id: String)

    // ---- bulk ----
    suspend fun snapshot(): DataSnapshot
    /** Replaces the whole database with [snapshot]; ids are kept as given. */
    suspend fun replaceAll(snapshot: DataSnapshot)
    /** Appends [snapshot] (ids must already be unique) after the existing categories. */
    suspend fun appendAll(snapshot: DataSnapshot)
}
