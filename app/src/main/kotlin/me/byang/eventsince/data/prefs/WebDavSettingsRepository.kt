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
package me.byang.eventsince.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureStore: SecureStore,
) {
    private object Keys {
        val URL = stringPreferencesKey("webdav_url")
        val USERNAME = stringPreferencesKey("webdav_username")
        val PASSWORD = stringPreferencesKey("webdav_password_enc")
        val REMOTE_DIR = stringPreferencesKey("webdav_remote_dir")
        val TRUST_ALL = booleanPreferencesKey("webdav_trust_all")
        val AUTO_BACKUP = booleanPreferencesKey("webdav_auto_backup")
        val WIFI_ONLY = booleanPreferencesKey("webdav_wifi_only")
        val KEEP_COUNT = intPreferencesKey("webdav_keep_count")
        val LAST_BACKUP_AT = longPreferencesKey("webdav_last_backup_at")
        val LAST_BACKUP_ERROR = stringPreferencesKey("webdav_last_backup_error")
    }

    val settings: Flow<WebDavSettings> = dataStore.data.map { p ->
        WebDavSettings(
            url = p[Keys.URL] ?: "",
            username = p[Keys.USERNAME] ?: "",
            password = secureStore.decrypt(p[Keys.PASSWORD] ?: ""),
            remoteDir = p[Keys.REMOTE_DIR] ?: "/EventSince/",
            trustAllCertificates = p[Keys.TRUST_ALL] ?: false,
            autoBackup = p[Keys.AUTO_BACKUP] ?: false,
            wifiOnly = p[Keys.WIFI_ONLY] ?: false,
            keepCount = p[Keys.KEEP_COUNT] ?: 10,
            lastBackupAt = p[Keys.LAST_BACKUP_AT],
            lastBackupError = p[Keys.LAST_BACKUP_ERROR],
        )
    }

    suspend fun current(): WebDavSettings = settings.first()

    suspend fun saveConnection(url: String, username: String, password: String, remoteDir: String, trustAll: Boolean) =
        dataStore.edit {
            it[Keys.URL] = url.trim()
            it[Keys.USERNAME] = username.trim()
            it[Keys.PASSWORD] = secureStore.encrypt(password)
            it[Keys.REMOTE_DIR] = normaliseDir(remoteDir)
            it[Keys.TRUST_ALL] = trustAll
        }

    suspend fun setAutoBackup(enabled: Boolean) = dataStore.edit { it[Keys.AUTO_BACKUP] = enabled }
    suspend fun setWifiOnly(enabled: Boolean) = dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    suspend fun setKeepCount(count: Int) = dataStore.edit { it[Keys.KEEP_COUNT] = count.coerceIn(1, 100) }

    suspend fun recordBackupResult(at: Long?, error: String?) = dataStore.edit {
        if (at != null) it[Keys.LAST_BACKUP_AT] = at
        if (error == null) it.remove(Keys.LAST_BACKUP_ERROR) else it[Keys.LAST_BACKUP_ERROR] = error
    }

    companion object {
        fun normaliseDir(raw: String): String {
            val trimmed = raw.trim().trim('/')
            return if (trimmed.isEmpty()) "/" else "/$trimmed/"
        }
    }
}
