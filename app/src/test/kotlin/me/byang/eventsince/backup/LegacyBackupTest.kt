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

import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.domain.model.LogType
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.domain.model.SortOrder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegacyBackupTest {
    private val legacy = """
        {
          "categories": [
            {
              "id": "cat-1", "name": "Habits", "sort": "DESC",
              "timers": [
                {
                  "id": "t-1", "label": "No coffee",
                  "time": 1700000000000, "stop": 0,
                  "color": 3, "created": 1690000000000,
                  "format": ["hours", "days"],
                  "records": [
                    { "id": "r-1", "time": 1690000000000, "elapsed": 86400000,
                      "label": "No coffee", "color": "#FFA229",
                      "created": 1700000000000, "notes": "slipped" }
                  ],
                  "notifications": [
                    { "id": "n-1", "timeUnit": "weeks", "target": "30", "body": "Has reached 30 weeks" },
                    { "id": "n-2", "timeUnit": "days", "target": "0" }
                  ]
                },
                {
                  "id": "t-2", "label": null, "time": 1650000000000, "stop": 1660000000000,
                  "color": 99, "created": 1650000000000, "records": []
                }
              ]
            },
            { "id": "cat-2", "name": null, "timers": [ { "id": "t-3", "time": 1600000000000, "created": 1600000000000 } ] }
          ],
          "widgetUpdate": false
        }
    """.trimIndent()

    @Test
    fun `detects and maps a legacy backup`() {
        val parsed = BackupCodec.parse(legacy.toByteArray(), importedAt = 1_800_000_000_000L)
        assertEquals(BackupKind.LEGACY, parsed.kind)
        val s = parsed.snapshot

        assertEquals(listOf("cat-1", "cat-2"), s.categories.map { it.id })
        assertEquals(SortOrder.DESC, s.categories[0].sort)
        assertEquals(SortOrder.ASC, s.categories[1].sort)
        assertTrue(s.categories[0].isDefault)
        assertEquals("", s.categories[1].name)

        assertEquals(3, s.events.size)
        val t1 = s.events.first { it.id == "t-1" }
        assertEquals(1700000000000L, t1.startAt)
        assertNull(t1.stoppedAt)
        assertEquals(1690000000000L, t1.createdAt)
        assertEquals(setOf(DurationUnit.DAYS, DurationUnit.HOURS), t1.format)
        assertEquals(3, t1.colorIndex)

        val t2 = s.events.first { it.id == "t-2" }
        assertEquals("", t2.label)
        assertEquals(1660000000000L, t2.stoppedAt)
        assertEquals(14, t2.colorIndex)
        assertNull(s.events.first { it.id == "t-3" }.format)

        val t1Logs = s.logs.filter { it.eventId == "t-1" }
        assertEquals(1, t1Logs.count { it.type == LogType.CREATED })
        assertEquals(1690000000000L, t1Logs.first { it.type == LogType.CREATED }.at)
        val reset = t1Logs.first { it.type == LogType.RESET }
        assertEquals("r-1", reset.id)
        assertEquals(1700000000000L, reset.at)
        assertEquals(1690000000000L, reset.previousStartAt)
        assertEquals(1700000000000L, reset.newStartAt)
        assertEquals(86400000L, reset.elapsedMs)
        assertEquals("#FFA229", reset.colorHex)
        assertEquals("slipped", reset.notes)

        val t2Logs = s.logs.filter { it.eventId == "t-2" }
        val stopped = t2Logs.first { it.type == LogType.STOPPED }
        assertEquals(1660000000000L, stopped.at)
        assertEquals(10000000000L, stopped.elapsedMs)

        assertEquals(1, s.reminders.size)
        val n = s.reminders[0]
        assertEquals("n-1", n.id)
        assertEquals(30, n.target)
        assertEquals(ReminderUnit.WEEKS, n.unit)
        assertEquals("Has reached 30 weeks", n.body)

        assertEquals(2, parsed.categoryCount)
        assertEquals(3, parsed.eventCount)
        assertEquals(1, parsed.resetCount)
    }

    @Test
    fun `rejects garbage`() {
        fun parse(s: String) = runCatching { BackupCodec.parse(s.toByteArray(), 0) }.exceptionOrNull()
        assertTrue(parse("not json") is BackupParseException)
        assertTrue(parse("[1,2]") is BackupParseException)
        assertTrue(parse("{\"foo\": 1}") is BackupParseException)
        assertTrue(parse("{\"schemaVersion\": 1, \"categories\": []}") is PlainJsonBackupException)
        assertTrue(parse("PK\u0003\u0004garbage") is BackupParseException)
    }
}
