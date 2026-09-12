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
package me.byang.eventsince.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import me.byang.eventsince.R
import me.byang.eventsince.core.color.Contrast
import me.byang.eventsince.core.time.ElapsedFormatter
import me.byang.eventsince.core.time.Heatmap
import me.byang.eventsince.core.time.RelativeTime
import me.byang.eventsince.data.prefs.WeekStart
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.LogType
import me.byang.eventsince.ui.common.ConfirmDialog
import me.byang.eventsince.ui.common.SectionHeader
import me.byang.eventsince.ui.common.formatDateTime
import me.byang.eventsince.ui.common.rememberNow
import me.byang.eventsince.ui.common.rememberUnitSuffixes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

/** Items in the history list that precede the log rows: heatmap, statistics, filter chips. */
private const val HEADER_ITEMS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var scrollTarget by remember { mutableStateOf<String?>(null) }
    var highlighted by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<EventLog?>(null) }
    var showClear by rememberSaveable { mutableStateOf(false) }
    val tip = stringResource(R.string.history_long_press_tip)

    LaunchedEffect(state.loaded) {
        if (state.loaded && state.resets.isNotEmpty()) snackbar.showSnackbar(tip)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showClear = true }, enabled = state.resets.isNotEmpty()) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.history_clear))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val event = state.event
        if (!state.loaded || event == null) return@Scaffold
        val suffixes = rememberUnitSuffixes()
        val now by rememberNow(60_000L)
        val locale = LocalConfiguration.current.locales[0]
        val weekStart = when (state.weekStart) {
            WeekStart.SUNDAY -> DayOfWeek.SUNDAY
            WeekStart.MONDAY -> DayOfWeek.MONDAY
            WeekStart.SYSTEM -> WeekFields.of(locale).firstDayOfWeek
        }
        val zone = remember { ZoneId.systemDefault() }
        val weeks = remember(state.resets, event.createdAt, weekStart) {
            Heatmap.build(state.resets.map { it.at }, event.createdAt, LocalDate.now(zone), weekStart, zone)
        }

        // Scroll to the requested record once it is part of the visible list. The target is
        // cleared only after the scroll finished, otherwise restarting the effect would cancel it.
        LaunchedEffect(scrollTarget) {
            val target = scrollTarget ?: return@LaunchedEffect
            val index = snapshotFlow { viewModel.uiState.value.visibleLogs.indexOfFirst { it.id == target } }
                .filter { it >= 0 }
                .first()
            listState.animateScrollToItem(HEADER_ITEMS + index)
            highlighted = target
            scrollTarget = null
        }
        LaunchedEffect(highlighted) {
            if (highlighted != null) {
                delay(2_000)
                highlighted = null
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                HeatmapView(
                    weeks = weeks,
                    weekStart = weekStart,
                    dateFormat = state.dateFormat,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    onCellClick = { cell ->
                        // Newest first in the list, so the day's first reset is the last one of that day.
                        val first = state.resets
                            .filter { java.time.Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == cell.date }
                            .minByOrNull { it.at } ?: return@HeatmapView
                        if (state.filter != LogFilter.ALL && state.filter != LogFilter.RESET) viewModel.setFilter(LogFilter.ALL)
                        scrollTarget = first.id
                    },
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val count = state.resets.size
                    Text(
                        pluralStringResource(R.plurals.history_reset_count, count, count),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.longestStreak?.let { log ->
                        val start = log.previousStartAt ?: 0L
                        val text = ElapsedFormatter.format(start, start + (log.elapsedMs ?: 0L), state.format, suffixes)
                        Text(
                            stringResource(R.string.history_longest_streak, text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                SectionHeader(stringResource(R.string.history_log))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LogFilter.entries.forEach { f ->
                        FilterChip(
                            selected = state.filter == f,
                            onClick = { viewModel.setFilter(f) },
                            label = { Text(f.displayName()) },
                        )
                    }
                }
            }
            if (state.visibleLogs.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.visibleLogs, key = { it.id }) { log ->
                LogRow(
                    log = log,
                    now = now,
                    state = state,
                    highlighted = log.id == highlighted,
                    onLongPress = { if (log.type == LogType.RESET) editing = log },
                )
            }
        }
    }

    editing?.let { log ->
        EditResetDialog(
            log = log,
            onSave = { viewModel.updateNotes(log.id, it) },
            onDelete = { viewModel.deleteLog(log.id) },
            onDismiss = { editing = null },
        )
    }
    if (showClear) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear),
            text = stringResource(R.string.history_clear_confirm),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = viewModel::clearResets,
            onDismiss = { showClear = false },
        )
    }
}

@Composable
private fun LogFilter.displayName(): String = stringResource(
    when (this) {
        LogFilter.ALL -> R.string.history_filter_all
        LogFilter.RESET -> R.string.history_filter_reset
        LogFilter.CREATED -> R.string.history_filter_created
        LogFilter.STOPPED -> R.string.history_filter_stopped
        LogFilter.EDITED -> R.string.history_filter_edited
        LogFilter.OTHER -> R.string.history_filter_other
    },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogRow(log: EventLog, now: Long, state: HistoryUiState, highlighted: Boolean, onLongPress: () -> Unit) {
    val suffixes = rememberUnitSuffixes()
    val dateTime = formatDateTime(log.at, state.dateFormat)
    val ago = RelativeTime.format(now, log.at, suffixes)?.let { stringResource(R.string.history_ago, it) }
        ?: stringResource(R.string.history_just_now)

    val highlight = if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlight)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (log.type) {
            LogType.RESET -> {
                val bg = Contrast.parseHex(log.colorHex) ?: 0xFF9E9E9E.toInt()
                val start = log.previousStartAt ?: log.at
                Box(
                    modifier = Modifier
                        .background(Color(bg), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        ElapsedFormatter.format(start, start + (log.elapsedMs ?: 0L), state.format, suffixes),
                        color = Color(Contrast.textColorFor(bg)),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            else -> {
                val icon: ImageVector = when (log.type) {
                    LogType.CREATED -> Icons.Default.AddCircle
                    LogType.STOPPED -> Icons.Default.Stop
                    LogType.EDITED -> Icons.Default.Edit
                    LogType.ARCHIVED -> Icons.Default.Archive
                    LogType.UNARCHIVED -> Icons.Default.Unarchive
                    LogType.IMPORTED -> Icons.Default.CloudDownload
                    LogType.RESET -> Icons.Default.Edit
                }
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(logTitle(log, state), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("$dateTime · $ago", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            log.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun logTitle(log: EventLog, state: HistoryUiState): String = when (log.type) {
    LogType.RESET -> stringResource(R.string.history_type_reset)
    LogType.CREATED -> stringResource(R.string.history_type_created, formatDateTime(log.newStartAt ?: log.at, state.dateFormat))
    LogType.STOPPED -> {
        val suffixes = rememberUnitSuffixes()
        val start = log.previousStartAt ?: log.at
        stringResource(R.string.history_type_stopped, ElapsedFormatter.format(start, start + (log.elapsedMs ?: 0L), state.format, suffixes))
    }
    LogType.EDITED -> stringResource(R.string.history_type_edited, editedFields(log.payload))
    LogType.ARCHIVED -> stringResource(R.string.history_type_archived)
    LogType.UNARCHIVED -> stringResource(R.string.history_type_unarchived)
    LogType.IMPORTED -> stringResource(R.string.history_type_imported, importSource(log.payload))
}

@Composable
private fun editedFields(payload: String?): String {
    val keys = remember(payload) {
        runCatching { Json.parseToJsonElement(payload ?: "{}").jsonObject.keys.toList() }.getOrDefault(emptyList())
    }
    val names = keys.map {
        when (it) {
            "label" -> stringResource(R.string.event_label)
            "color" -> stringResource(R.string.event_color)
            "category" -> stringResource(R.string.event_category)
            "format" -> stringResource(R.string.event_format)
            else -> it
        }
    }
    return names.joinToString(", ")
}

private fun importSource(payload: String?): String =
    runCatching { Json.parseToJsonElement(payload ?: "{}").jsonObject["source"]?.toString()?.trim('"') }.getOrNull() ?: ""

@Composable
private fun EditResetDialog(log: EventLog, onSave: (String?) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var notes by rememberSaveable { mutableStateOf(log.notes ?: "") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_edit_reset)) },
        text = {
            Column {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.history_notes)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.history_delete_record), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes); onDismiss() }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.history_delete_record),
            text = stringResource(R.string.history_delete_record_confirm),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { onDelete(); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}
