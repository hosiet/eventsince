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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R
import me.byang.eventsince.backup.BackupKind
import me.byang.eventsince.backup.BackupManager
import me.byang.eventsince.backup.ImportMode
import me.byang.eventsince.backup.ParsedBackup
import me.byang.eventsince.ui.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupManager.MIME_TYPE)) { uri ->
        uri?.let(viewModel::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::loadPreview)
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val text = when (message) {
            BackupMessage.Exported -> resources.getString(R.string.backup_exported)
            is BackupMessage.Imported -> resources.getString(R.string.backup_imported, message.events)
            BackupMessage.InvalidFile -> resources.getString(R.string.backup_invalid_file)
            BackupMessage.PlainJsonRejected -> resources.getString(R.string.backup_plain_json_rejected)
            is BackupMessage.Error -> resources.getString(R.string.backup_error, message.detail)
        }
        viewModel.consumeMessage()
        scope.launch { snackbar.showSnackbar(text) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            SectionHeader(stringResource(R.string.backup_export_section))
            Text(
                stringResource(R.string.backup_export_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Button(
                enabled = !state.busy,
                onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Text(stringResource(R.string.backup_export), modifier = Modifier.padding(start = 8.dp))
            }

            SectionHeader(stringResource(R.string.backup_import_section), Modifier.padding(top = 16.dp))
            Text(
                stringResource(R.string.backup_import_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedButton(
                enabled = !state.busy,
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Text(stringResource(R.string.backup_import), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    state.preview?.let { parsed ->
        ImportPreviewDialog(
            parsed = parsed,
            onImport = viewModel::import,
            onDismiss = viewModel::dismissPreview,
        )
    }
}

@Composable
fun ImportPreviewDialog(parsed: ParsedBackup, onImport: (ImportMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when (parsed.kind) {
                        BackupKind.EVENTSINCE -> stringResource(R.string.backup_preview_kind_eventsince, parsed.schemaVersion)
                        BackupKind.LEGACY -> stringResource(R.string.backup_preview_kind_legacy)
                    },
                )
                Text(stringResource(R.string.backup_preview_counts, parsed.categoryCount, parsed.eventCount, parsed.resetCount, parsed.reminderCount))
                Text(
                    stringResource(R.string.backup_preview_modes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(ImportMode.MERGE) }) { Text(stringResource(R.string.backup_import_merge)) }
        },
        dismissButton = {
            TextButton(onClick = { onImport(ImportMode.REPLACE) }) {
                Text(stringResource(R.string.backup_import_replace), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}
