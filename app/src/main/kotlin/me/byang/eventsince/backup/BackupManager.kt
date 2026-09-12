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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.byang.eventsince.BuildConfig
import me.byang.eventsince.R
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.Clock
import me.byang.eventsince.domain.DataChangeBus
import me.byang.eventsince.domain.ReminderScheduler
import me.byang.eventsince.domain.model.DataSnapshot
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.LogType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ImportMode { REPLACE, MERGE }

/** Where a backup came from; recorded in the IMPORTED log entries. */
enum class ImportSource(val key: String) { BACKUP("backup"), LEGACY("legacy"), WEBDAV("webdav") }

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: EventRepository,
    private val preferences: PreferencesRepository,
    private val scheduler: ReminderScheduler,
    private val bus: DataChangeBus,
    private val clock: Clock,
) {
    fun suggestedFileName(now: Long = clock.now()): String =
        "eventsince-" + FILE_STAMP.format(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())) + FILE_EXTENSION

    /** The complete backup as a zip archive. */
    suspend fun export(): ByteArray {
        val app = AppInfo(context.getString(R.string.app_name), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        return BackupCodec.encodeZip(repository.snapshot(), preferences.current(), app, clock.now())
    }

    fun parse(bytes: ByteArray): ParsedBackup = BackupCodec.parse(bytes, clock.now())

    suspend fun import(parsed: ParsedBackup, mode: ImportMode, source: ImportSource) {
        val effectiveSource = if (parsed.kind == BackupKind.LEGACY) ImportSource.LEGACY else source
        val now = clock.now()
        when (mode) {
            ImportMode.REPLACE -> {
                repository.getAllReminders().forEach { scheduler.cancelReminder(it.id) }
                repository.replaceAll(withImportLogs(parsed.snapshot, effectiveSource, now))
                parsed.preferences?.let { preferences.restore(it) }
            }
            ImportMode.MERGE -> {
                repository.appendAll(withImportLogs(regenerateIds(parsed.snapshot), effectiveSource, now))
            }
        }
        scheduler.rescheduleAll()
        bus.notifyChanged()
    }

    private fun withImportLogs(snapshot: DataSnapshot, source: ImportSource, now: Long): DataSnapshot {
        val payload = buildJsonObject { put("source", source.key) }.toString()
        val extra = snapshot.events.map { e ->
            EventLog(UUID.randomUUID().toString(), e.id, LogType.IMPORTED, at = now, payload = payload)
        }
        return snapshot.copy(logs = snapshot.logs + extra)
    }

    companion object {
        const val FILE_EXTENSION = ".zip"
        const val MIME_TYPE = "application/zip"
        private val FILE_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

        /** Gives every entity a fresh id while keeping the references between them intact. */
        fun regenerateIds(snapshot: DataSnapshot): DataSnapshot {
            val categoryIds = snapshot.categories.associate { it.id to UUID.randomUUID().toString() }
            val eventIds = snapshot.events.associate { it.id to UUID.randomUUID().toString() }
            return DataSnapshot(
                categories = snapshot.categories.map { it.copy(id = categoryIds.getValue(it.id), isDefault = false) },
                events = snapshot.events.map {
                    it.copy(id = eventIds.getValue(it.id), categoryId = categoryIds[it.categoryId] ?: it.categoryId)
                },
                logs = snapshot.logs.mapNotNull { l ->
                    eventIds[l.eventId]?.let { l.copy(id = UUID.randomUUID().toString(), eventId = it) }
                },
                reminders = snapshot.reminders.mapNotNull { r ->
                    eventIds[r.eventId]?.let { r.copy(id = UUID.randomUUID().toString(), eventId = it) }
                },
            )
        }
    }
}
