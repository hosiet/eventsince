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
package me.byang.eventsince.ui.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.core.time.ElapsedFormatter
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.ui.common.ConfirmDialog
import me.byang.eventsince.ui.common.DurationChip
import me.byang.eventsince.ui.common.SectionHeader
import me.byang.eventsince.ui.common.StartDateTimePicker
import me.byang.eventsince.ui.common.displayLabel
import me.byang.eventsince.ui.common.formatDateTime
import me.byang.eventsince.ui.common.rememberNow
import me.byang.eventsince.ui.common.rememberUnitSuffixes
import me.byang.eventsince.ui.theme.LocalEventPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onFormat: (String) -> Unit,
    onHistory: (String) -> Unit,
    onReminders: (String) -> Unit,
    onShare: (String) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val closed by viewModel.closed.collectAsStateWithLifecycle()
    val palette = LocalEventPalette.current
    var showResetPicker by rememberSaveable { mutableStateOf(false) }
    var showArchiveConfirm by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(closed) { if (closed) onBack() }
    LaunchedEffect(state.loaded, state.event) { if (state.loaded && state.event == null) onBack() }

    val event = state.event
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.event_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (event != null) {
                        IconButton(onClick = { onShare(event.id) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_title))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (event == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            EventHeroCard(event = event, format = state.format)
            event.archivedAt?.let {
                Text(
                    text = stringResource(R.string.event_archived_at, formatDateTime(it, LocalDateFormat.current)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            SectionHeader(stringResource(R.string.event_section_actions))
            ActionRow(Icons.Default.Refresh, stringResource(R.string.event_reset_now)) {
                viewModel.reset(null, palette.hex(event.colorIndex))
            }
            ActionRow(Icons.Default.EventRepeat, stringResource(R.string.event_reset_from_date)) { showResetPicker = true }
            ActionRow(Icons.Default.Stop, stringResource(R.string.event_stop), enabled = event.isRunning) { viewModel.stop() }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.event_section_options))
            ActionRow(Icons.Default.Edit, stringResource(R.string.event_edit)) { onEdit(event.id) }
            ActionRow(Icons.Default.Tune, stringResource(R.string.event_format)) { onFormat(event.id) }
            if (event.isArchived) {
                ActionRow(Icons.Default.Unarchive, stringResource(R.string.event_unarchive)) { viewModel.unarchive() }
            } else {
                ActionRow(Icons.Default.Archive, stringResource(R.string.event_archive)) { showArchiveConfirm = true }
            }
            ActionRow(Icons.Default.DeleteForever, stringResource(R.string.event_delete), destructive = true) { showDeleteConfirm = true }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.event_section_more))
            ActionRow(Icons.Default.History, stringResource(R.string.history_title)) { onHistory(event.id) }
            ActionRow(Icons.Default.Notifications, stringResource(R.string.reminders_title)) { onReminders(event.id) }

            Spacer(Modifier.height(16.dp))
            val dateFormat = LocalDateFormat.current
            Text(
                text = stringResource(R.string.event_started_at, formatDateTime(event.startAt, dateFormat)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            event.stoppedAt?.let {
                Text(
                    text = stringResource(R.string.event_stopped_at, formatDateTime(it, dateFormat)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }

    if (showResetPicker && event != null) {
        StartDateTimePicker(
            initialMillis = null,
            onDismiss = { showResetPicker = false },
            onPicked = {
                viewModel.reset(it, palette.hex(event.colorIndex))
                showResetPicker = false
            },
        )
    }
    if (showArchiveConfirm && event != null) {
        ConfirmDialog(
            title = stringResource(R.string.event_archive),
            text = stringResource(R.string.event_archive_confirm, event.displayLabel()),
            confirmLabel = stringResource(R.string.action_archive),
            onConfirm = { showArchiveConfirm = false; viewModel.archive() },
            onDismiss = { showArchiveConfirm = false },
        )
    }
    if (showDeleteConfirm && event != null) {
        ConfirmDialog(
            title = stringResource(R.string.event_delete),
            text = stringResource(R.string.event_delete_confirm, event.displayLabel()),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { showDeleteConfirm = false; viewModel.delete() },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
fun EventHeroCard(event: Event, format: Set<DurationUnit>, modifier: Modifier = Modifier) {
    val palette = LocalEventPalette.current
    val suffixes = rememberUnitSuffixes()
    val now by rememberNow(ElapsedFormatter.refreshIntervalMillis(format))
    Card(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).alpha(if (event.isRunning) 1f else 0.6f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DurationChip(
                text = ElapsedFormatter.format(event.startAt, event.endAt(now), format, suffixes),
                background = palette.background(event.colorIndex),
                foreground = palette.foreground(event.colorIndex),
                fontSize = 28.sp,
                minFontSize = 18.sp,
                maxLines = 2,
                horizontalPadding = 20,
                verticalPadding = 10,
            )
            Text(
                text = event.displayLabel(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        headlineContent = { Text(text, color = tint) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        modifier = Modifier.then(
            if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
        ),
    )
}
