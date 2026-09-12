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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import me.byang.eventsince.data.prefs.UserPreferences
import me.byang.eventsince.domain.model.DataSnapshot
import me.byang.eventsince.domain.model.LogType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class BackupKind { EVENTSINCE, LEGACY }

open class BackupParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** An EventSince backup in the retired plain-JSON form; only the zip container is accepted. */
class PlainJsonBackupException : BackupParseException("plain JSON EventSince backups are not supported")

/** A parsed backup, ready to be previewed or imported. */
data class ParsedBackup(
    val kind: BackupKind,
    val schemaVersion: Int,
    val snapshot: DataSnapshot,
    val preferences: UserPreferences?,
    val exportedAt: Long?,
) {
    val categoryCount: Int get() = snapshot.categories.size
    val eventCount: Int get() = snapshot.events.count { it.archivedAt == null }
    val resetCount: Int get() = snapshot.logs.count { it.type == LogType.RESET }
    val reminderCount: Int get() = snapshot.reminders.size
}

/**
 * Serialises and parses backup files; pure Kotlin so it can be unit-tested.
 *
 * EventSince backups are zip archives holding `manifest.json` (a small header) and
 * `backup.json` (the data, compact). Plain-JSON EventSince files are rejected; the original
 * app's plain-JSON `.backup` files are still accepted.
 */
object BackupCodec {
    const val ENTRY_MANIFEST = "manifest.json"
    const val ENTRY_DATA = "backup.json"
    const val FORMAT_NAME = "eventsince-backup"
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Serializable
    data class Manifest(val format: String = FORMAT_NAME, val schemaVersion: Int, val exportedAt: Long, val app: AppInfo)

    /** The JSON payload alone (what goes into `backup.json`). */
    fun encodeJson(snapshot: DataSnapshot, preferences: UserPreferences, app: AppInfo, exportedAt: Long): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(exportedAt = exportedAt, app = app, preferences = preferences.toDto(), categories = snapshot.toDtos()),
        )

    /** A complete backup archive. */
    fun encodeZip(snapshot: DataSnapshot, preferences: UserPreferences, app: AppInfo, exportedAt: Long): ByteArray {
        val manifest = json.encodeToString(Manifest.serializer(), Manifest(schemaVersion = BackupFile.CURRENT_SCHEMA, exportedAt = exportedAt, app = app))
        val data = encodeJson(snapshot, preferences, app, exportedAt)
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.setLevel(Deflater.BEST_COMPRESSION)
            for ((name, text) in listOf(ENTRY_MANIFEST to manifest, ENTRY_DATA to data)) {
                zip.putNextEntry(ZipEntry(name).apply { time = exportedAt })
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= ZIP_MAGIC.size && ZIP_MAGIC.indices.all { bytes[it] == ZIP_MAGIC[it] }

    /** Parses either an EventSince zip archive or a legacy plain-JSON backup. */
    fun parse(bytes: ByteArray, importedAt: Long): ParsedBackup {
        if (isZip(bytes)) return parseJson(readDataEntry(bytes), importedAt, fromArchive = true)
        return parseJson(String(bytes, Charsets.UTF_8), importedAt, fromArchive = false)
    }

    private fun readDataEntry(bytes: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == ENTRY_DATA) return zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        throw BackupParseException("archive has no $ENTRY_DATA")
    }

    private fun parseJson(text: String, importedAt: Long, fromArchive: Boolean): ParsedBackup {
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: Exception) {
            throw BackupParseException("not valid JSON", e)
        }
        val obj = root as? JsonObject ?: throw BackupParseException("not a JSON object")
        if (!fromArchive) {
            if (LegacyBackup.isLegacy(obj)) {
                return ParsedBackup(BackupKind.LEGACY, 0, LegacyBackup.toSnapshot(obj, importedAt), null, null)
            }
            if (obj.containsKey("schemaVersion")) throw PlainJsonBackupException()
            throw BackupParseException("unknown JSON file")
        }
        val version = obj["schemaVersion"]?.jsonPrimitive?.intOrNull
            ?: throw BackupParseException("missing schemaVersion")
        if (version > BackupFile.CURRENT_SCHEMA) throw BackupParseException("schema $version is newer than supported ${BackupFile.CURRENT_SCHEMA}")
        val file = try {
            json.decodeFromJsonElement(BackupFile.serializer(), obj)
        } catch (e: Exception) {
            throw BackupParseException("unexpected structure", e)
        }
        return ParsedBackup(
            kind = BackupKind.EVENTSINCE,
            schemaVersion = version,
            snapshot = file.categories.toSnapshot(),
            preferences = file.preferences?.toPreferences(),
            exportedAt = file.exportedAt,
        )
    }

}
