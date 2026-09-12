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

import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.prefs.DarkMode
import me.byang.eventsince.data.prefs.UserPreferences
import me.byang.eventsince.data.prefs.WeekStart
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.DataSnapshot
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.LogType
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.domain.model.SortOrder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackupCodecTest {
    private val snapshot = DataSnapshot(
        categories = listOf(
            Category("c1", "", 0, SortOrder.ASC, isDefault = true),
            Category("c2", "Work", 1, SortOrder.DESC, isDefault = false),
        ),
        events = listOf(
            Event("e1", "c1", "Coffee", 1_000L, null, 4, 500L, setOf(DurationUnit.DAYS), 0),
            Event("e2", "c2", "", 2_000L, 3_000L, 0, 2_000L, null, 0, archivedAt = 4_000L),
        ),
        logs = listOf(
            EventLog("l1", "e1", LogType.CREATED, 500L, newStartAt = 500L),
            EventLog("l2", "e1", LogType.RESET, 1_000L, previousStartAt = 500L, newStartAt = 1_000L, elapsedMs = 500L, labelSnapshot = "Coffee", colorHex = "#FFBBBB", notes = "n"),
            EventLog("l3", "e2", LogType.EDITED, 2_500L, payload = "{\"label\":{\"from\":\"a\",\"to\":\"b\"}}"),
        ),
        reminders = listOf(Reminder("r1", "e1", 3, ReminderUnit.WEEKS, null, 600L)),
    )
    private val prefs = UserPreferences(
        darkMode = DarkMode.DARK, dynamicColor = false, paletteKey = "ocean",
        format = setOf(DurationUnit.YEARS, DurationUnit.DAYS), dateFormat = DateFormatPreference.DAY_MONTH_YEAR,
        weekStart = WeekStart.MONDAY,
    )

    @Test
    fun `round trip through the zip container preserves everything`() {
        val bytes = BackupCodec.encodeZip(snapshot, prefs, AppInfo("EventSince", "0.1.0", 1), exportedAt = 42L)
        assertTrue(BackupCodec.isZip(bytes))
        val entries = java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.map { it.name }.toList()
        }
        assertEquals(listOf(BackupCodec.ENTRY_MANIFEST, BackupCodec.ENTRY_DATA), entries)
        val parsed = BackupCodec.parse(bytes, importedAt = 99L)

        assertEquals(BackupKind.EVENTSINCE, parsed.kind)
        assertEquals(42L, parsed.exportedAt)
        assertEquals(prefs, parsed.preferences)
        assertEquals(snapshot.categories, parsed.snapshot.categories)
        assertEquals(snapshot.events.sortedBy { it.id }, parsed.snapshot.events.sortedBy { it.id })
        assertEquals(snapshot.logs.sortedBy { it.id }, parsed.snapshot.logs.sortedBy { it.id })
        assertEquals(snapshot.reminders, parsed.snapshot.reminders)
        assertEquals(1, parsed.eventCount) // deleted events are not counted
    }

    @Test
    fun `unknown palette in preferences falls back to default`() {
        val bytes = BackupCodec.encodeZip(snapshot, prefs.copy(paletteKey = "nope"), AppInfo("x", "1"), 0)
        assertEquals("default", BackupCodec.parse(bytes, 0).preferences?.paletteKey)
    }

    @Test
    fun `compressed archive is much smaller than the json`() {
        val text = BackupCodec.encodeJson(snapshot, prefs, AppInfo("x", "1"), 0)
        val bytes = BackupCodec.encodeZip(snapshot, prefs, AppInfo("x", "1"), 0)
        assertTrue(bytes.size < text.length, "zip ${bytes.size} vs json ${text.length}")
    }

    @Test
    fun `plain json EventSince backups are rejected`() {
        val text = BackupCodec.encodeJson(snapshot, prefs, AppInfo("x", "1"), 0)
        assertTrue(runCatching { BackupCodec.parse(text.toByteArray(), 0) }.exceptionOrNull() is PlainJsonBackupException)
    }

    @Test
    fun `regenerated ids keep references`() {
        val fresh = BackupManager.regenerateIds(snapshot)
        assertNotEquals(snapshot.events[0].id, fresh.events[0].id)
        assertTrue(fresh.categories.none { it.isDefault })
        val c1 = fresh.categories[0].id
        assertEquals(c1, fresh.events.first { it.label == "Coffee" }.categoryId)
        val e1 = fresh.events.first { it.label == "Coffee" }.id
        assertEquals(2, fresh.logs.count { it.eventId == e1 })
        assertEquals(e1, fresh.reminders.single().eventId)
        assertEquals(snapshot.logs.size, fresh.logs.size)
    }
}
