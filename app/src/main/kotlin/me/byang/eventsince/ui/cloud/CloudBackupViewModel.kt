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
package me.byang.eventsince.ui.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.backup.BackupParseException
import me.byang.eventsince.backup.ImportMode
import me.byang.eventsince.backup.ParsedBackup
import me.byang.eventsince.backup.PlainJsonBackupException
import me.byang.eventsince.data.prefs.WebDavSettings
import me.byang.eventsince.data.prefs.WebDavSettingsRepository
import me.byang.eventsince.webdav.AutoBackupScheduler
import me.byang.eventsince.webdav.CloudBackupService
import me.byang.eventsince.webdav.RemoteFile
import javax.inject.Inject

sealed interface CloudMessage {
    data object Saved : CloudMessage
    data object ConnectionOk : CloudMessage
    data class Uploaded(val name: String) : CloudMessage
    data class Restored(val events: Int) : CloudMessage
    data object InvalidFile : CloudMessage
    data object PlainJsonRejected : CloudMessage
    data class Error(val detail: String) : CloudMessage
}

data class CloudUiState(
    val busy: Boolean = false,
    val remoteFiles: List<RemoteFile>? = null,
    val preview: ParsedBackup? = null,
    val message: CloudMessage? = null,
)

@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    private val settingsRepository: WebDavSettingsRepository,
    private val service: CloudBackupService,
    private val scheduler: AutoBackupScheduler,
) : ViewModel() {

    val settings: StateFlow<WebDavSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(CloudUiState())
    val uiState: StateFlow<CloudUiState> = _uiState.asStateFlow()

    private fun run(block: suspend () -> CloudMessage?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true)
            val message = try {
                block()
            } catch (e: PlainJsonBackupException) {
                CloudMessage.PlainJsonRejected
            } catch (e: BackupParseException) {
                CloudMessage.InvalidFile
            } catch (e: Exception) {
                CloudMessage.Error(e.message ?: e.toString())
            }
            _uiState.value = _uiState.value.copy(busy = false, message = message)
        }
    }

    fun saveConnection(url: String, username: String, password: String, remoteDir: String, trustAll: Boolean) = run {
        settingsRepository.saveConnection(url, username, password, remoteDir, trustAll)
        CloudMessage.Saved
    }

    fun testConnection(url: String, username: String, password: String, remoteDir: String, trustAll: Boolean) = run {
        service.testConnection(
            WebDavSettings(
                url = url.trim(), username = username.trim(), password = password,
                remoteDir = WebDavSettingsRepository.normaliseDir(remoteDir), trustAllCertificates = trustAll,
            ),
        )
        CloudMessage.ConnectionOk
    }

    fun uploadNow() = run { CloudMessage.Uploaded(service.upload()) }

    fun listRemote() = run {
        val files = service.listBackups()
        _uiState.value = _uiState.value.copy(remoteFiles = files)
        null
    }

    fun dismissRemote() {
        _uiState.value = _uiState.value.copy(remoteFiles = null)
    }

    fun download(file: RemoteFile) = run {
        val parsed = service.download(file)
        _uiState.value = _uiState.value.copy(remoteFiles = null, preview = parsed)
        null
    }

    fun dismissPreview() {
        _uiState.value = _uiState.value.copy(preview = null)
    }

    fun restore(mode: ImportMode) {
        val parsed = _uiState.value.preview ?: return
        _uiState.value = _uiState.value.copy(preview = null)
        run {
            service.restore(parsed, mode)
            CloudMessage.Restored(parsed.eventCount)
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackup(enabled)
            if (enabled) scheduler.scheduleIfEnabled() else scheduler.cancel()
        }
    }

    fun setWifiOnly(enabled: Boolean) = viewModelScope.launch { settingsRepository.setWifiOnly(enabled) }
    fun setKeepCount(count: Int) = viewModelScope.launch { settingsRepository.setKeepCount(count) }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

}
