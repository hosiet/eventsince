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

enum class LogType {
    CREATED, RESET, STOPPED, EDITED, ARCHIVED, UNARCHIVED, IMPORTED;

    companion object {
        /** Older data called archiving "DELETED"; it maps onto [ARCHIVED]. */
        fun fromKey(key: String): LogType? =
            if (key == "DELETED") ARCHIVED else entries.firstOrNull { it.name == key }
    }
}

/**
 * One entry of an event's operation log. Which optional fields are set depends on [type]:
 *
 * - CREATED: [newStartAt]
 * - RESET: [previousStartAt], [elapsedMs], [newStartAt], [labelSnapshot], [colorHex], [notes]
 * - STOPPED: [previousStartAt] (start of the stopped period), [elapsedMs]; [at] is the stop instant
 * - EDITED: [payload] (JSON summary of before/after)
 * - DELETED: none
 * - IMPORTED: [payload] (JSON `{"source": "backup|legacy|webdav"}`)
 */
data class EventLog(
    val id: String,
    val eventId: String,
    val type: LogType,
    val at: Long,
    val previousStartAt: Long? = null,
    val newStartAt: Long? = null,
    val elapsedMs: Long? = null,
    val labelSnapshot: String? = null,
    val colorHex: String? = null,
    val notes: String? = null,
    val payload: String? = null,
)
