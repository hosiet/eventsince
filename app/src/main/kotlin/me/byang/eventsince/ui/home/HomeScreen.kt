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
package me.byang.eventsince.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R
import me.byang.eventsince.core.time.ElapsedFormatter
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.SortOrder
import me.byang.eventsince.ui.common.DurationChip
import me.byang.eventsince.ui.common.displayLabel
import me.byang.eventsince.ui.common.displayName
import me.byang.eventsince.ui.common.rememberNow
import me.byang.eventsince.ui.common.rememberUnitSuffixes
import me.byang.eventsince.ui.theme.LocalEventPalette
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddEvent: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (searching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::setQuery,
                            placeholder = { Text(stringResource(R.string.home_search_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { searching = false; viewModel.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_stat_event),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.app_name))
                        }
                    },
                    actions = {
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.home_search))
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEvent) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_event))
            }
        },
    ) { padding ->
        when {
            !state.loaded -> Box(Modifier.fillMaxSize().padding(padding))
            state.isEmpty && state.isFiltering -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.home_search_empty), style = MaterialTheme.typography.bodyLarge)
            }
            state.isEmpty -> EmptyHome(Modifier.padding(padding), onAddEvent)
            else -> HomeList(
                state = state,
                contentPadding = padding,
                onOpenEvent = onOpenEvent,
                onOpenCategories = onOpenCategories,
                onToggleSort = viewModel::toggleSort,
                onToggleArchived = viewModel::toggleArchived,
                onLayoutChanged = viewModel::applyLayout,
            )
        }
    }
}

@Composable
private fun EmptyHome(modifier: Modifier, onAddEvent: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onAddEvent) { Text(stringResource(R.string.home_add_event)) }
    }
}

@Composable
private fun HomeList(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onOpenEvent: (String) -> Unit,
    onOpenCategories: () -> Unit,
    onToggleSort: (String) -> Unit,
    onToggleArchived: () -> Unit,
    onLayoutChanged: (List<HomeRow>) -> Unit,
) {
    val rows = remember(state.rows) { mutableStateListOf<HomeRow>().apply { addAll(state.rows) } }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.key == from.key }
        val toIndex = rows.indexOfFirst { it.key == to.key }
        if (fromIndex < 0 || toIndex < 0) return@rememberReorderableLazyListState
        // The first header stays pinned so that every event has a category above it.
        if (toIndex == 0 && rows[0] is HomeRow.Header) return@rememberReorderableLazyListState
        // Nothing moves into or below the archive section.
        if (rows[toIndex].isArchive) return@rememberReorderableLazyListState
        rows.add(toIndex, rows.removeAt(fromIndex))
    }
    val now by rememberNow(if (state.hasSeconds) 1_000L else 60_000L)
    val suffixes = rememberUnitSuffixes()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is HomeRow.Header -> ReorderableItem(reorderState, key = row.key, enabled = rows.firstOrNull() != row && !state.isFiltering) { dragging ->
                    val draggable = rows.firstOrNull() != row && !state.isFiltering
                    CategoryHeaderRow(
                        category = row.category,
                        draggable = draggable,
                        dragHandle = Modifier.draggableHandle(
                            enabled = draggable,
                            onDragStopped = { onLayoutChanged(rows.toList()) },
                        ),
                        onClickName = onOpenCategories,
                        onToggleSort = { onToggleSort(row.category.id) },
                        modifier = Modifier
                            .shadow(if (dragging) 4.dp else 0.dp)
                            .longPressDraggableHandle(enabled = draggable, onDragStopped = { onLayoutChanged(rows.toList()) }),
                    )
                }
                is HomeRow.Item -> ReorderableItem(reorderState, key = row.key, enabled = !state.isFiltering) { dragging ->
                    EventRow(
                        event = row.event,
                        elapsed = ElapsedFormatter.format(
                            row.event.startAt, row.event.endAt(now), row.event.effectiveFormat(state.globalFormat), suffixes,
                        ),
                        dragHandle = Modifier.draggableHandle(
                            enabled = !state.isFiltering,
                            onDragStopped = { onLayoutChanged(rows.toList()) },
                        ),
                        onClick = { onOpenEvent(row.event.id) },
                        modifier = Modifier
                            .shadow(if (dragging) 4.dp else 0.dp)
                            .longPressDraggableHandle(enabled = !state.isFiltering, onDragStopped = { onLayoutChanged(rows.toList()) }),
                    )
                }
                is HomeRow.ArchiveHeader -> ReorderableItem(reorderState, key = row.key, enabled = false) {
                    ArchiveHeaderRow(count = row.count, expanded = row.expanded, onClick = onToggleArchived)
                }
                is HomeRow.Archived -> ReorderableItem(reorderState, key = row.key, enabled = false) {
                    EventRow(
                        event = row.event,
                        elapsed = ElapsedFormatter.format(
                            row.event.startAt, row.event.endAt(now), row.event.effectiveFormat(state.globalFormat), suffixes,
                        ),
                        dragHandle = null,
                        onClick = { onOpenEvent(row.event.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveHeaderRow(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Same slot as the category drag handle so the titles line up.
        Icon(
            Icons.Default.Archive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp).alpha(0.6f),
        )
        Text(
            text = pluralStringResource(R.plurals.home_archived, count, count),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        IconButton(onClick = onClick) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.home_archived_collapse else R.string.home_archived_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryHeaderRow(
    category: Category,
    draggable: Boolean,
    dragHandle: Modifier,
    onClickName: () -> Unit,
    onToggleSort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = null,
                modifier = dragHandle.padding(8.dp).alpha(if (draggable) 0.6f else 0f),
            )
            Text(
                text = category.displayName(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).clickable(onClick = onClickName).padding(vertical = 8.dp),
            )
            IconButton(onClick = onToggleSort) {
                Icon(
                    if (category.sort == SortOrder.ASC) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.home_sort_by_start),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One event of the list; [dragHandle] is null for archived events, which cannot be reordered. */
@Composable
private fun EventRow(
    event: Event,
    elapsed: String,
    dragHandle: Modifier?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalEventPalette.current
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .alpha(if (event.isRunning && !event.isArchived) 1f else 0.5f)
                .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dragHandle != null) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.home_drag_handle),
                    modifier = dragHandle.padding(8.dp).alpha(0.6f),
                )
            } else {
                // Keeps archived rows aligned with the draggable ones.
                Spacer(Modifier.size(40.dp))
            }
            ChipAndLabel(
                chip = {
                    DurationChip(
                        text = elapsed,
                        background = palette.background(event.colorIndex),
                        foreground = palette.foreground(event.colorIndex),
                        fontSize = 15.sp,
                    )
                },
                label = {
                    Text(
                        text = event.displayLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Places [chip] and [label] side by side while the label keeps a reasonable share of the row.
 * When a long duration format leaves too little room, the label moves below the chip instead
 * so neither is cut off.
 */
@Composable
private fun ChipAndLabel(
    chip: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(chip, label), modifier = modifier) { (chipMeasurables, labelMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val chipPlaceable = chipMeasurables.first().measure(loose)
        val labelMeasurable = labelMeasurables.first()
        val gap = 12.dp.roundToPx()
        val remaining = constraints.maxWidth - chipPlaceable.width - gap
        val labelFitsOnOneLine = remaining >= labelMeasurable.maxIntrinsicWidth(constraints.maxHeight)
        val sideBySide = labelFitsOnOneLine || remaining >= constraints.maxWidth / 2
        if (sideBySide) {
            val labelPlaceable = labelMeasurable.measure(loose.copy(maxWidth = remaining.coerceAtLeast(0)))
            val height = maxOf(chipPlaceable.height, labelPlaceable.height)
            layout(constraints.maxWidth, height) {
                chipPlaceable.placeRelative(0, (height - chipPlaceable.height) / 2)
                labelPlaceable.placeRelative(chipPlaceable.width + gap, (height - labelPlaceable.height) / 2)
            }
        } else {
            val labelPlaceable = labelMeasurable.measure(loose)
            val stackGap = 6.dp.roundToPx()
            val height = chipPlaceable.height + stackGap + labelPlaceable.height
            layout(constraints.maxWidth, height) {
                chipPlaceable.placeRelative(0, 0)
                labelPlaceable.placeRelative(0, chipPlaceable.height + stackGap)
            }
        }
    }
}

