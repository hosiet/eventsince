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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.ui.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormatScreen(
    onDone: () -> Unit,
    viewModel: EventFormatViewModel = hiltViewModel(),
) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val globalFormat by viewModel.globalFormat.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()

    var initialised by rememberSaveable { mutableStateOf(false) }
    var useGlobal by rememberSaveable { mutableStateOf(true) }
    var unitKeys by rememberSaveable { mutableStateOf(DurationUnit.toKeys(DurationUnit.DEFAULT)) }
    val units = DurationUnit.parse(unitKeys)

    LaunchedEffect(event, globalFormat) {
        val e = event ?: return@LaunchedEffect
        if (!initialised) {
            useGlobal = e.format == null
            unitKeys = DurationUnit.toKeys(e.format ?: globalFormat)
            initialised = true
        }
    }
    LaunchedEffect(done) { if (done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.event_format_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(enabled = initialised, onClick = { viewModel.save(if (useGlobal) null else units) }) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.event_format_use_global)) },
                supportingContent = { Text(stringResource(R.string.event_format_use_global_hint)) },
                trailingContent = {
                    Switch(checked = useGlobal, onCheckedChange = {
                        useGlobal = it
                        if (it) unitKeys = DurationUnit.toKeys(globalFormat)
                    })
                },
                modifier = Modifier.clickable {
                    useGlobal = !useGlobal
                    if (useGlobal) unitKeys = DurationUnit.toKeys(globalFormat)
                },
            )
            SectionHeader(stringResource(R.string.format_units), Modifier.padding(top = 8.dp))
            FormatEditor(
                units = units,
                enabled = !useGlobal,
                onChange = { unitKeys = DurationUnit.toKeys(it) },
            )
        }
    }
}
