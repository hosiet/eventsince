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

import me.byang.eventsince.core.time.DurationUnit

data class Event(
    val id: String,
    val categoryId: String,
    /** Empty when the user gave no label; the UI shows a localised fallback. */
    val label: String,
    /** Start of the current period (changes on reset). */
    val startAt: Long,
    /** Null while running; the stop instant once stopped. */
    val stoppedAt: Long?,
    /** Index 0..14 into the active event palette. */
    val colorIndex: Int,
    /** Start of the very first period; never changes. */
    val createdAt: Long,
    /** Per-event unit override; null means "use the global format". */
    val format: Set<DurationUnit>?,
    val position: Int,
    /** Set while the event is archived: kept in the database and in backups, hidden from the main list. */
    val archivedAt: Long? = null,
) {
    val isRunning: Boolean get() = stoppedAt == null
    val isArchived: Boolean get() = archivedAt != null

    /** End of the period to display: the stop instant, or [now] while running. */
    fun endAt(now: Long): Long = stoppedAt ?: now

    fun effectiveFormat(global: Set<DurationUnit>): Set<DurationUnit> = format ?: global
}

data class CategoryWithEvents(val category: Category, val events: List<Event>)
