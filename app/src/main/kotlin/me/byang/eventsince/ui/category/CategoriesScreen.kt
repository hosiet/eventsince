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
package me.byang.eventsince.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.CategoryDeletion
import me.byang.eventsince.ui.common.ConfirmDialog
import me.byang.eventsince.ui.common.TextInputDialog
import me.byang.eventsince.ui.common.displayName
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<CategoryRow?>(null) }

    val order = remember(rows) { mutableStateListOf<CategoryRow>().apply { addAll(rows) } }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = order.indexOfFirst { it.category.id == from.key }
        val toIndex = order.indexOfFirst { it.category.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) order.add(toIndex, order.removeAt(fromIndex))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.category_new))
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            items(order, key = { it.category.id }) { row ->
                ReorderableItem(reorderState, key = row.category.id) { dragging ->
                    Surface(
                        modifier = Modifier
                            .shadow(if (dragging) 4.dp else 0.dp)
                            .longPressDraggableHandle(onDragStopped = { viewModel.reorder(order.map { it.category.id }) }),
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = stringResource(R.string.home_drag_handle),
                                    modifier = Modifier
                                        .draggableHandle(onDragStopped = { viewModel.reorder(order.map { it.category.id }) })
                                        .alpha(0.6f),
                                )
                            },
                            headlineContent = { Text(row.category.displayName()) },
                            supportingContent = {
                                Text(pluralStringResource(R.plurals.category_event_count, row.eventCount, row.eventCount))
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editing = row.category }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.category_rename))
                                    }
                                    IconButton(onClick = { deleting = row }, enabled = !row.category.isDefault) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.category_delete))
                                    }
                                }
                            },
                            modifier = Modifier.clickable { editing = row.category },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = stringResource(R.string.category_new),
            initial = "",
            label = stringResource(R.string.category_name),
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = viewModel::create,
            onDismiss = { showCreate = false },
        )
    }
    editing?.let { category ->
        TextInputDialog(
            title = stringResource(R.string.category_rename),
            initial = category.name,
            label = stringResource(R.string.category_name),
            allowBlank = category.isDefault,
            onConfirm = { viewModel.rename(category.id, it) },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { row ->
        if (row.eventCount == 0) {
            ConfirmDialog(
                title = stringResource(R.string.category_delete),
                text = stringResource(R.string.category_delete_confirm, row.category.displayName()),
                confirmLabel = stringResource(R.string.action_delete),
                destructive = true,
                onConfirm = { viewModel.delete(row.category.id, CategoryDeletion.MoveEvents(null)) },
                onDismiss = { deleting = null },
            )
        } else {
            DeleteCategoryDialog(
                row = row,
                others = rows.map { it.category }.filter { it.id != row.category.id },
                onConfirm = { viewModel.delete(row.category.id, it) },
                onDismiss = { deleting = null },
            )
        }
    }
}

private enum class DeleteChoice { DELETE_EVENTS, MOVE_TO_DEFAULT, MOVE_TO_OTHER }

@Composable
private fun DeleteCategoryDialog(
    row: CategoryRow,
    others: List<Category>,
    onConfirm: (CategoryDeletion) -> Unit,
    onDismiss: () -> Unit,
) {
    val nonDefaultOthers = others.filter { !it.isDefault }
    var choice by remember { mutableStateOf(DeleteChoice.MOVE_TO_DEFAULT) }
    var target by remember { mutableStateOf(nonDefaultOthers.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_delete)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    pluralStringResource(R.plurals.category_delete_has_events, row.eventCount, row.category.displayName(), row.eventCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                ChoiceRow(stringResource(R.string.category_delete_move_default), choice == DeleteChoice.MOVE_TO_DEFAULT) {
                    choice = DeleteChoice.MOVE_TO_DEFAULT
                }
                if (nonDefaultOthers.isNotEmpty()) {
                    ChoiceRow(stringResource(R.string.category_delete_move_other), choice == DeleteChoice.MOVE_TO_OTHER) {
                        choice = DeleteChoice.MOVE_TO_OTHER
                    }
                    if (choice == DeleteChoice.MOVE_TO_OTHER) {
                        nonDefaultOthers.forEach { c ->
                            ChoiceRow(c.displayName(), target == c.id, indent = true) { target = c.id }
                        }
                    }
                }
                ChoiceRow(stringResource(R.string.category_delete_with_events), choice == DeleteChoice.DELETE_EVENTS) {
                    choice = DeleteChoice.DELETE_EVENTS
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val deletion = when (choice) {
                    DeleteChoice.DELETE_EVENTS -> CategoryDeletion.DeleteEvents
                    DeleteChoice.MOVE_TO_DEFAULT -> CategoryDeletion.MoveEvents(null)
                    DeleteChoice.MOVE_TO_OTHER -> CategoryDeletion.MoveEvents(target)
                }
                onConfirm(deletion)
                onDismiss()
            }) {
                Text(
                    stringResource(R.string.action_delete),
                    color = if (choice == DeleteChoice.DELETE_EVENTS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ChoiceRow(text: String, selected: Boolean, indent: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = if (indent) 24.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
