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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.ui.common.SectionHeader
import me.byang.eventsince.ui.common.StartDateTimePicker
import me.byang.eventsince.ui.common.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    onDone: () -> Unit,
    viewModel: AddEventViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val createdCategoryId by viewModel.createdCategoryId.collectAsStateWithLifecycle()

    var label by rememberSaveable { mutableStateOf("") }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var categoryId by rememberSaveable { mutableStateOf(viewModel.initialCategoryId) }
    var startAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val placeholder = rememberRandomPlaceholder()

    LaunchedEffect(done) { if (done) onDone() }
    LaunchedEffect(createdCategoryId) { createdCategoryId?.let { categoryId = it } }
    LaunchedEffect(categories) {
        if (categoryId == null || categories.none { it.id == categoryId }) {
            categoryId = (categories.firstOrNull { it.isDefault } ?: categories.firstOrNull())?.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_event_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            EventFormFields(
                label = label,
                onLabelChange = { label = it },
                labelPlaceholder = placeholder,
                colorIndex = colorIndex,
                onColorChange = { colorIndex = it },
                categories = categories,
                selectedCategoryId = categoryId,
                onCategoryChange = { categoryId = it },
                onCreateCategory = viewModel::createCategory,
            )

            SectionHeader(stringResource(R.string.event_start))
            StartCard(
                startAt = startAt,
                onPick = { showPicker = true },
                onResetToNow = { startAt = null },
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.create(label, colorIndex, categoryId, startAt) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) { Text(stringResource(R.string.add_event_start)) }
        }
    }

    if (showPicker) {
        StartDateTimePicker(
            initialMillis = startAt,
            onDismiss = { showPicker = false },
            onPicked = { picked ->
                startAt = picked.coerceAtMost(System.currentTimeMillis())
                showPicker = false
            },
        )
    }
}

@Composable
fun StartCard(startAt: Long?, onPick: () -> Unit, onResetToNow: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onPick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = if (startAt == null) stringResource(R.string.event_start_now)
                    else formatDateTime(startAt, LocalDateFormat.current),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.event_start_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (startAt != null) {
                TextButton(onClick = onResetToNow) { Text(stringResource(R.string.event_start_use_now)) }
            }
        }
    }
}

/** Date format preference for the whole UI, provided by the activity. */
val LocalDateFormat = androidx.compose.runtime.staticCompositionLocalOf { DateFormatPreference.SYSTEM }
