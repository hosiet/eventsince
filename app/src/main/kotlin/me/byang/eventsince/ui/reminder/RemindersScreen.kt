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
package me.byang.eventsince.ui.reminder

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.byang.eventsince.R
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.ui.common.ConfirmDialog
import me.byang.eventsince.ui.common.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Reminder?>(null) }

    fun hasPermission() =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(hasPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    // SCHEDULE_EXACT_ALARM is granted from a system settings page rather than a dialog, so it is
    // only re-checked when the screen comes back to the foreground.
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    var exactAlarmsAllowed by remember { mutableStateOf(alarmManager.canScheduleExactAlarms()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = hasPermission()
                exactAlarmsAllowed = alarmManager.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }
    val limitMessage = stringResource(R.string.reminders_limit_reached)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminders_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                showAdd = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.reminders_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.reminders_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    stringResource(R.string.reminders_battery_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                )
            }
            if (!granted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.NotificationsOff, contentDescription = null)
                            Column {
                                Text(stringResource(R.string.reminders_permission_denied), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                    )
                                }) { Text(stringResource(R.string.reminders_open_settings)) }
                            }
                        }
                    }
                }
            }
            if (!exactAlarmsAllowed) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.AlarmOff, contentDescription = null)
                            Column {
                                Text(stringResource(R.string.reminders_exact_alarms_off), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)),
                                    )
                                }) { Text(stringResource(R.string.reminders_allow_exact_alarms)) }
                            }
                        }
                    }
                }
            }
            if (state.loaded && state.reminders.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.reminders_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.reminders, key = { it.id }) { reminder ->
                val event = state.event
                val fireAt = event?.let { reminder.fireAt(it.startAt) }
                val supporting = when {
                    event == null -> ""
                    !event.isRunning -> stringResource(R.string.reminders_paused_stopped)
                    fireAt != null && fireAt <= System.currentTimeMillis() -> stringResource(R.string.reminders_already_passed)
                    fireAt != null -> stringResource(R.string.reminders_fires_at, formatDateTime(fireAt, state.dateFormat))
                    else -> ""
                }
                ListItem(
                    headlineContent = { Text(reminder.describe()) },
                    supportingContent = { Text(supporting) },
                    trailingContent = {
                        IconButton(onClick = { deleting = reminder }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    },
                )
            }
        }
    }

    if (showAdd) {
        AddReminderDialog(
            onAdd = { target, unit ->
                viewModel.add(target, unit) { scope.launch { snackbar.showSnackbar(limitMessage) } }
            },
            onDismiss = { showAdd = false },
        )
    }
    deleting?.let { reminder ->
        ConfirmDialog(
            title = stringResource(R.string.reminders_delete),
            text = stringResource(R.string.reminders_delete_confirm, reminder.describe()),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { viewModel.delete(reminder.id) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun Reminder.describe(): String = pluralStringResource(
    when (unit) {
        ReminderUnit.HOURS -> R.plurals.reminder_target_hours
        ReminderUnit.DAYS -> R.plurals.reminder_target_days
        ReminderUnit.WEEKS -> R.plurals.reminder_target_weeks
        ReminderUnit.YEARS -> R.plurals.reminder_target_years
    },
    target, target,
)

@Composable
private fun ReminderUnit.displayName(): String = stringResource(
    when (this) {
        ReminderUnit.HOURS -> R.string.unit_hours
        ReminderUnit.DAYS -> R.string.unit_days
        ReminderUnit.WEEKS -> R.string.unit_weeks
        ReminderUnit.YEARS -> R.string.unit_years
    },
)

@Composable
private fun AddReminderDialog(onAdd: (Int, ReminderUnit) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(ReminderUnit.DAYS) }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(6) },
                    label = { Text(stringResource(R.string.reminders_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderUnit.entries.forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u.displayName()) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = value != null && value > 0, onClick = { onAdd(value ?: 0, unit); onDismiss() }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
