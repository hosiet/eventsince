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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R
import me.byang.eventsince.ui.backup.ImportPreviewDialog
import me.byang.eventsince.ui.common.SectionHeader
import me.byang.eventsince.ui.common.formatDateTime
import me.byang.eventsince.ui.event.LocalDateFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupScreen(
    onBack: () -> Unit,
    viewModel: CloudBackupViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    var initialised by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var remoteDir by rememberSaveable { mutableStateOf("/EventSince/") }
    var trustAll by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings) {
        val s = settings ?: return@LaunchedEffect
        if (!initialised) {
            url = s.url; username = s.username; password = s.password; remoteDir = s.remoteDir; trustAll = s.trustAllCertificates
            initialised = true
        }
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val text = when (message) {
            CloudMessage.Saved -> resources.getString(R.string.cloud_saved)
            CloudMessage.ConnectionOk -> resources.getString(R.string.cloud_connection_ok)
            is CloudMessage.Uploaded -> resources.getString(R.string.cloud_uploaded, message.name)
            is CloudMessage.Restored -> resources.getString(R.string.backup_imported, message.events)
            CloudMessage.InvalidFile -> resources.getString(R.string.backup_invalid_file)
            CloudMessage.PlainJsonRejected -> resources.getString(R.string.backup_plain_json_rejected)
            is CloudMessage.Error -> resources.getString(R.string.backup_error, message.detail)
        }
        viewModel.consumeMessage()
        scope.launch { snackbar.showSnackbar(text) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cloud_backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s = settings ?: return@Scaffold
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            SectionHeader(stringResource(R.string.cloud_server_section))
            OutlinedTextField(
                value = url, onValueChange = { url = it }, singleLine = true,
                label = { Text(stringResource(R.string.cloud_url)) },
                placeholder = { Text("https://cloud.example.com/remote.php/dav/files/user/") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            OutlinedTextField(
                value = username, onValueChange = { username = it }, singleLine = true,
                label = { Text(stringResource(R.string.cloud_username)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it }, singleLine = true,
                label = { Text(stringResource(R.string.cloud_password)) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            OutlinedTextField(
                value = remoteDir, onValueChange = { remoteDir = it }, singleLine = true,
                label = { Text(stringResource(R.string.cloud_remote_dir)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.cloud_trust_all)) },
                supportingContent = { Text(stringResource(R.string.cloud_trust_all_hint)) },
                trailingContent = { Switch(checked = trustAll, onCheckedChange = { trustAll = it }) },
                modifier = Modifier.clickable { trustAll = !trustAll },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = !state.busy && url.isNotBlank(),
                    onClick = { viewModel.testConnection(url, username, password, remoteDir, trustAll) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.cloud_test)) }
                Button(
                    enabled = !state.busy,
                    onClick = { viewModel.saveConnection(url, username, password, remoteDir, trustAll) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.cloud_manual_section))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(enabled = !state.busy && s.isConfigured, onClick = viewModel::uploadNow, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_upload_now))
                }
                OutlinedButton(enabled = !state.busy && s.isConfigured, onClick = viewModel::listRemote, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_restore))
                }
            }
            val dateFormat = LocalDateFormat.current
            Text(
                text = when {
                    s.lastBackupError != null -> stringResource(R.string.cloud_last_error, s.lastBackupError)
                    s.lastBackupAt != null -> stringResource(R.string.cloud_last_backup, formatDateTime(s.lastBackupAt, dateFormat))
                    else -> stringResource(R.string.cloud_never_backed_up)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (s.lastBackupError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.cloud_auto_section))
            ListItem(
                headlineContent = { Text(stringResource(R.string.cloud_auto_backup)) },
                supportingContent = { Text(stringResource(R.string.cloud_auto_backup_hint)) },
                trailingContent = {
                    Switch(checked = s.autoBackup, enabled = s.isConfigured, onCheckedChange = viewModel::setAutoBackup)
                },
                modifier = Modifier.clickable(enabled = s.isConfigured) { viewModel.setAutoBackup(!s.autoBackup) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.cloud_wifi_only)) },
                trailingContent = { Switch(checked = s.wifiOnly, onCheckedChange = viewModel::setWifiOnly) },
                modifier = Modifier.clickable { viewModel.setWifiOnly(!s.wifiOnly) },
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.cloud_keep_count, s.keepCount), style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = s.keepCount.toFloat(),
                    onValueChange = { viewModel.setKeepCount(it.roundToInt()) },
                    valueRange = 1f..50f,
                    steps = 48,
                )
            }
        }
    }

    state.remoteFiles?.let { files ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRemote,
            title = { Text(stringResource(R.string.cloud_restore)) },
            text = {
                if (files.isEmpty()) {
                    Text(stringResource(R.string.cloud_no_backups))
                } else {
                    val dateFormat = LocalDateFormat.current
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(files, key = { it.href }) { file ->
                            ListItem(
                                headlineContent = { Text(file.name) },
                                supportingContent = {
                                    val parts = listOfNotNull(
                                        file.lastModified?.let { formatDateTime(it, dateFormat) },
                                        file.size?.let { "${it / 1024} KB" },
                                    )
                                    Text(parts.joinToString(" · "))
                                },
                                modifier = Modifier.clickable { viewModel.download(file) },
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissRemote) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    state.preview?.let { parsed ->
        ImportPreviewDialog(parsed = parsed, onImport = viewModel::restore, onDismiss = viewModel::dismissPreview)
    }
}
