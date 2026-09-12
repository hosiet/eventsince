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
package me.byang.eventsince.domain.model

/** Everything in the database, used for backup export/import. */
data class DataSnapshot(
    val categories: List<Category>,
    val events: List<Event>,
    val logs: List<EventLog>,
    val reminders: List<Reminder>,
) {
    /**
     * Moves the events of this snapshot's default category into the category [targetId] and
     * drops that default category, so merging a backup does not add a second "default" category.
     * Moved events are renumbered from [firstPosition] to sort after the existing ones.
     */
    fun foldDefaultCategoryInto(targetId: String, firstPosition: Int): DataSnapshot {
        val imported = categories.firstOrNull { it.isDefault } ?: return this
        var next = firstPosition
        return copy(
            categories = categories.filter { it.id != imported.id },
            events = events.map { e ->
                if (e.categoryId == imported.id) e.copy(categoryId = targetId, position = next++) else e
            },
        )
    }
}

/** What to do with the events of a category that is being deleted. */
sealed interface CategoryDeletion {
    /** Soft-delete the events together with the category. */
    data object DeleteEvents : CategoryDeletion
    /** Move the events to another category (the default one when [targetCategoryId] is null). */
    data class MoveEvents(val targetCategoryId: String?) : CategoryDeletion
}

/** Placement of one event after a drag-and-drop on the home screen. */
data class EventPlacement(val eventId: String, val categoryId: String, val position: Int)
