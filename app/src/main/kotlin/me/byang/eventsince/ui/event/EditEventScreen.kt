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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    onDone: () -> Unit,
    viewModel: EditEventViewModel = hiltViewModel(),
) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val createdCategoryId by viewModel.createdCategoryId.collectAsStateWithLifecycle()

    var initialised by rememberSaveable { mutableStateOf(false) }
    var label by rememberSaveable { mutableStateOf("") }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        if (!initialised) {
            label = e.label
            colorIndex = e.colorIndex
            categoryId = e.categoryId
            initialised = true
        }
    }
    LaunchedEffect(done) { if (done) onDone() }
    LaunchedEffect(createdCategoryId) { createdCategoryId?.let { categoryId = it } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_event_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(
                        enabled = initialised,
                        onClick = { viewModel.save(label, colorIndex, categoryId ?: event?.categoryId ?: return@TextButton) },
                    ) { Text(stringResource(R.string.action_save)) }
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
        ) {
            Spacer(Modifier.height(8.dp))
            EventFormFields(
                label = label,
                onLabelChange = { label = it },
                labelPlaceholder = stringResource(R.string.event_untitled),
                colorIndex = colorIndex,
                onColorChange = { colorIndex = it },
                categories = categories,
                selectedCategoryId = categoryId,
                onCategoryChange = { categoryId = it },
                onCreateCategory = viewModel::createCategory,
            )
        }
    }
}
