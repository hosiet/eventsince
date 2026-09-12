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
package me.byang.eventsince.domain

import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.CategoryDeletion
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventPlacement
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use-case layer: every mutation goes through here so that alarms are kept in sync
 * and interested parties (widgets, auto backup) learn about the change.
 */
@Singleton
class EventService @Inject constructor(
    private val repository: EventRepository,
    private val scheduler: ReminderScheduler,
    private val bus: DataChangeBus,
    private val clock: Clock,
) {
    // ---- events ----

    suspend fun createEvent(label: String, colorIndex: Int, categoryId: String?, requestedStartAt: Long?): Event {
        val now = clock.now()
        val start = ResetSemantics.clampStart(requestedStartAt, now)
        return repository.createEvent(label, colorIndex, categoryId, start, now).also { bus.notifyChanged() }
    }

    suspend fun resetEvent(id: String, requestedStartAt: Long?, colorHex: String): Event? {
        val event = repository.resetEvent(id, requestedStartAt, colorHex, clock.now())
        scheduler.rescheduleEvent(id)
        bus.notifyChanged()
        return event
    }

    suspend fun stopEvent(id: String): Event? {
        val event = repository.stopEvent(id, clock.now())
        scheduler.cancelEvent(id)
        bus.notifyChanged()
        return event
    }

    suspend fun editEvent(id: String, label: String, colorIndex: Int, categoryId: String): Event? {
        val before = repository.getEvent(id)
        val after = repository.editEvent(id, label, colorIndex, categoryId, clock.now())
        if (before != null && after != null && before.label != after.label) scheduler.rescheduleEvent(id)
        bus.notifyChanged()
        return after
    }

    suspend fun setEventFormat(id: String, format: Set<DurationUnit>?) {
        repository.setEventFormat(id, format, clock.now())
        bus.notifyChanged()
    }

    suspend fun applyFormatToAllEvents(format: Set<DurationUnit>) {
        repository.applyFormatToAllEvents(format)
        bus.notifyChanged()
    }

    suspend fun archiveEvent(id: String) {
        repository.archiveEvent(id, clock.now())
        scheduler.cancelEvent(id)
        bus.notifyChanged()
    }

    suspend fun unarchiveEvent(id: String) {
        repository.unarchiveEvent(id, clock.now())
        scheduler.rescheduleEvent(id)
        bus.notifyChanged()
    }

    /** Permanent: the event disappears from the database and from every backup made afterwards. */
    suspend fun deleteEvent(id: String) {
        scheduler.cancelEvent(id)
        repository.deleteEvent(id)
        bus.notifyChanged()
    }

    suspend fun applyHomeLayout(categoryOrder: List<String>, placements: List<EventPlacement>) {
        repository.applyHomeLayout(categoryOrder, placements)
        bus.notifyChanged()
    }

    // ---- categories ----

    suspend fun createCategory(name: String): Category = repository.createCategory(name).also { bus.notifyChanged() }

    suspend fun renameCategory(id: String, name: String) {
        repository.renameCategory(id, name)
        bus.notifyChanged()
    }

    suspend fun reorderCategories(orderedIds: List<String>) {
        repository.reorderCategories(orderedIds)
        bus.notifyChanged()
    }

    suspend fun toggleCategorySort(id: String) {
        repository.toggleCategorySort(id)
        bus.notifyChanged()
    }

    suspend fun deleteCategory(id: String, deletion: CategoryDeletion) {
        val deletedEvents = repository.deleteCategory(id, deletion, clock.now())
        deletedEvents.forEach { scheduler.cancelEvent(it) }
        bus.notifyChanged()
    }

    // ---- logs ----

    suspend fun updateLogNotes(logId: String, notes: String?) {
        repository.updateLogNotes(logId, notes)
        bus.notifyChanged()
    }

    suspend fun deleteLog(logId: String) {
        repository.deleteLog(logId)
        bus.notifyChanged()
    }

    suspend fun clearResetLogs(eventId: String) {
        repository.clearResetLogs(eventId)
        bus.notifyChanged()
    }

    // ---- reminders ----

    suspend fun addReminder(eventId: String, target: Int, unit: ReminderUnit): Reminder {
        val reminder = repository.addReminder(eventId, target, unit, clock.now())
        scheduler.rescheduleEvent(eventId)
        bus.notifyChanged()
        return reminder
    }

    suspend fun deleteReminder(id: String) {
        scheduler.cancelReminder(id)
        repository.deleteReminder(id)
        bus.notifyChanged()
    }
}
