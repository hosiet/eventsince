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

enum class ReminderUnit(val key: String, val millis: Long) {
    HOURS("hours", 3_600_000L),
    DAYS("days", 86_400_000L),
    WEEKS("weeks", 604_800_000L),
    YEARS("years", (365.2425 * 86_400_000L).toLong());

    companion object {
        fun fromKey(key: String?): ReminderUnit = entries.firstOrNull { it.key == key } ?: DAYS
    }
}

data class Reminder(
    val id: String,
    val eventId: String,
    val target: Int,
    val unit: ReminderUnit,
    /** Legacy frozen body from an imported backup; EventSince generates the text at fire time. */
    val body: String?,
    val createdAt: Long,
) {
    /** Instant at which the reminder fires for an event whose current period started at [startAt]. */
    fun fireAt(startAt: Long): Long = startAt + target * unit.millis
}
