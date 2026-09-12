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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.DataSnapshot
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.LogType
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.domain.model.SortOrder
import java.util.UUID

/**
 * Reads the backup file of the original "time since" app (top-level `categories`, no
 * `schemaVersion`) and maps it onto EventSince's model:
 *
 * - every timer gets a CREATED log at its `created` instant;
 * - `stop > 0` adds a STOPPED log;
 * - each `record` becomes a RESET log (`at = created`, `previousStartAt = time`,
 *   `elapsedMs = elapsed`, `colorHex = color`, `notes`);
 * - a missing `format` inherits the global one; a missing `sort` is ASC;
 * - notifications become reminders with the same id, unit, target and frozen body.
 *
 * Parsing is lenient: absent, null or oddly typed fields fall back to defaults.
 */
object LegacyBackup {

    fun isLegacy(root: JsonElement): Boolean {
        val obj = root as? JsonObject ?: return false
        return obj.containsKey("categories") && !obj.containsKey("schemaVersion")
    }

    fun toSnapshot(root: JsonObject, importedAt: Long): DataSnapshot {
        val categories = mutableListOf<Category>()
        val events = mutableListOf<Event>()
        val logs = mutableListOf<EventLog>()
        val reminders = mutableListOf<Reminder>()

        root["categories"]?.asArray().orEmpty().forEachIndexed { ci, cEl ->
            val c = cEl as? JsonObject ?: return@forEachIndexed
            val categoryId = c.str("id") ?: UUID.randomUUID().toString()
            categories += Category(
                id = categoryId,
                name = c.str("name").orEmpty(),
                position = ci,
                sort = SortOrder.fromKey(c.str("sort")),
                isDefault = ci == 0,
            )
            c["timers"]?.asArray().orEmpty().forEachIndexed { ti, tEl ->
                val t = tEl as? JsonObject ?: return@forEachIndexed
                val eventId = t.str("id") ?: UUID.randomUUID().toString()
                val time = t.long("time") ?: t.long("created") ?: importedAt
                val created = t.long("created") ?: time
                val stop = t.long("stop")?.takeIf { it > 0 }
                val formatKeys = t["format"]?.asArray()?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                events += Event(
                    id = eventId,
                    categoryId = categoryId,
                    label = t.str("label").orEmpty(),
                    startAt = time,
                    stoppedAt = stop,
                    colorIndex = (t.long("color") ?: 0L).toInt().coerceIn(0, 14),
                    createdAt = created,
                    format = formatKeys?.takeIf { it.isNotEmpty() }?.let { DurationUnit.parse(it) },
                    position = ti,
                )
                logs += EventLog(id = UUID.randomUUID().toString(), eventId = eventId, type = LogType.CREATED, at = created, newStartAt = created)
                t["records"]?.asArray().orEmpty().forEach { rEl ->
                    val r = rEl as? JsonObject ?: return@forEach
                    val recordCreated = r.long("created") ?: return@forEach
                    val recordTime = r.long("time") ?: return@forEach
                    logs += EventLog(
                        id = r.str("id") ?: UUID.randomUUID().toString(),
                        eventId = eventId,
                        type = LogType.RESET,
                        at = recordCreated,
                        previousStartAt = recordTime,
                        newStartAt = recordCreated,
                        elapsedMs = r.long("elapsed") ?: 0L,
                        labelSnapshot = r.str("label"),
                        colorHex = r.str("color"),
                        notes = r.str("notes"),
                    )
                }
                if (stop != null) {
                    logs += EventLog(
                        id = UUID.randomUUID().toString(), eventId = eventId, type = LogType.STOPPED, at = stop,
                        previousStartAt = time, elapsedMs = (stop - time).coerceAtLeast(0),
                    )
                }
                t["notifications"]?.asArray().orEmpty().forEach { nEl ->
                    val n = nEl as? JsonObject ?: return@forEach
                    val target = n.long("target")?.toInt() ?: return@forEach
                    if (target <= 0) return@forEach
                    reminders += Reminder(
                        id = n.str("id") ?: UUID.randomUUID().toString(),
                        eventId = eventId,
                        target = target,
                        unit = ReminderUnit.fromKey(n.str("timeUnit")),
                        body = n.str("body"),
                        createdAt = importedAt,
                    )
                }
            }
        }
        return DataSnapshot(categories, events, logs, reminders)
    }

    private fun JsonElement.asArray(): JsonArray? = this as? JsonArray

    private fun JsonObject.str(key: String): String? {
        val p = this[key] as? JsonPrimitive ?: return null
        if (p is JsonNull) return null
        return p.contentOrNull
    }

    private fun JsonObject.long(key: String): Long? {
        val p = this[key] as? JsonPrimitive ?: return null
        if (p is JsonNull) return null
        p.longOrNull?.let { return it }
        p.doubleOrNull?.let { return it.toLong() }
        p.booleanOrNull?.let { return if (it) 1L else 0L }
        return p.contentOrNull?.trim()?.toLongOrNull()
    }



}
