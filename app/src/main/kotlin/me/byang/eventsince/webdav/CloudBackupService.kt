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
package me.byang.eventsince.webdav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.byang.eventsince.backup.BackupManager
import me.byang.eventsince.backup.ImportMode
import me.byang.eventsince.backup.ImportSource
import me.byang.eventsince.backup.ParsedBackup
import me.byang.eventsince.data.prefs.WebDavSettings
import me.byang.eventsince.data.prefs.WebDavSettingsRepository
import me.byang.eventsince.domain.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Upload / list / restore of backups on the configured WebDAV server. */
@Singleton
class CloudBackupService @Inject constructor(
    private val settingsRepository: WebDavSettingsRepository,
    private val backupManager: BackupManager,
    private val clock: Clock,
) {
    private fun client(settings: WebDavSettings): WebDavClient {
        require(settings.isConfigured) { "WebDAV is not configured" }
        return WebDavClient(WebDavConfig(settings.url, settings.username, settings.password, settings.trustAllCertificates))
    }

    /** Verifies the credentials and that the remote directory exists or can be created. */
    suspend fun testConnection(settings: WebDavSettings) = withContext(Dispatchers.IO) {
        val client = client(settings)
        client.ensureDirectory(settings.remoteDir)
        client.list(settings.remoteDir)
        Unit
    }

    /** Uploads a fresh backup and prunes old ones; records the outcome in the settings. */
    suspend fun upload(): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.current()
        try {
            val client = client(settings)
            val name = backupManager.suggestedFileName()
            val bytes = backupManager.export()
            client.ensureDirectory(settings.remoteDir)
            client.put(settings.remoteDir + name, bytes, BackupManager.MIME_TYPE)
            prune(client, settings)
            settingsRepository.recordBackupResult(clock.now(), null)
            name
        } catch (e: Exception) {
            settingsRepository.recordBackupResult(null, e.message ?: e.toString())
            throw e
        }
    }

    /** Backup files in the remote directory, newest first. */
    suspend fun listBackups(): List<RemoteFile> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.current()
        client(settings).list(settings.remoteDir).filter { isBackupFile(it) }.sortedByDescending { it.name }
    }

    suspend fun download(file: RemoteFile): ParsedBackup = withContext(Dispatchers.IO) {
        val settings = settingsRepository.current()
        backupManager.parse(client(settings).get(settings.remoteDir + file.name))
    }

    suspend fun restore(parsed: ParsedBackup, mode: ImportMode) = backupManager.import(parsed, mode, ImportSource.WEBDAV)

    private fun prune(client: WebDavClient, settings: WebDavSettings) {
        val backups = client.list(settings.remoteDir).filter { isBackupFile(it) }.sortedByDescending { it.name }
        backups.drop(settings.keepCount.coerceAtLeast(1)).forEach { client.delete(settings.remoteDir + it.name) }
    }

    companion object {
        private val NAME = Regex("""eventsince-\d{8}-\d{4}\.zip""")
        fun isBackupFile(file: RemoteFile): Boolean = !file.isDirectory && NAME.matches(file.name)
    }
}
