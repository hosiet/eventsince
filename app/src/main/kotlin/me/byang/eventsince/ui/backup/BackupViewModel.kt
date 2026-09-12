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
package me.byang.eventsince.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.byang.eventsince.backup.BackupManager
import me.byang.eventsince.backup.BackupParseException
import me.byang.eventsince.backup.ImportMode
import me.byang.eventsince.backup.ImportSource
import me.byang.eventsince.backup.ParsedBackup
import me.byang.eventsince.backup.PlainJsonBackupException
import javax.inject.Inject

sealed interface BackupMessage {
    data object Exported : BackupMessage
    data class Imported(val events: Int) : BackupMessage
    data class Error(val detail: String) : BackupMessage
    data object InvalidFile : BackupMessage
    data object PlainJsonRejected : BackupMessage
}

data class BackupUiState(
    val busy: Boolean = false,
    val preview: ParsedBackup? = null,
    val message: BackupMessage? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: BackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun suggestedFileName(): String = manager.suggestedFileName()

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true)
            val result = runCatching {
                val bytes = manager.export()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                        ?: error("cannot open destination")
                }
            }
            _uiState.value = BackupUiState(
                message = result.fold({ BackupMessage.Exported }, { BackupMessage.Error(it.message ?: it.toString()) }),
            )
        }
    }

    fun loadPreview(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true)
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("cannot open file")
                }
                manager.parse(bytes)
            }
            _uiState.value = result.fold(
                { BackupUiState(preview = it) },
                {
                    BackupUiState(
                        message = when (it) {
                            is PlainJsonBackupException -> BackupMessage.PlainJsonRejected
                            is BackupParseException -> BackupMessage.InvalidFile
                            else -> BackupMessage.Error(it.message ?: it.toString())
                        },
                    )
                },
            )
        }
    }

    fun dismissPreview() {
        _uiState.value = _uiState.value.copy(preview = null)
    }

    fun import(mode: ImportMode) {
        val parsed = _uiState.value.preview ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, preview = null)
            val result = runCatching { manager.import(parsed, mode, ImportSource.BACKUP) }
            _uiState.value = BackupUiState(
                message = result.fold({ BackupMessage.Imported(parsed.eventCount) }, { BackupMessage.Error(it.message ?: it.toString()) }),
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
