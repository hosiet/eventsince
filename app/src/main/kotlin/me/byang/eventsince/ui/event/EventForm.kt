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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.byang.eventsince.R
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.ui.common.ColorGrid
import me.byang.eventsince.ui.common.SectionHeader
import me.byang.eventsince.ui.common.TextInputDialog
import me.byang.eventsince.ui.common.displayName
import me.byang.eventsince.ui.theme.LocalEventPalette

/** Label + colour + category fields shared by the add and edit screens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventFormFields(
    label: String,
    onLabelChange: (String) -> Unit,
    labelPlaceholder: String,
    colorIndex: Int,
    onColorChange: (Int) -> Unit,
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategoryChange: (String) -> Unit,
    onCreateCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalEventPalette.current
    var showNewCategory by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = label,
            onValueChange = onLabelChange,
            label = { Text(stringResource(R.string.event_label)) },
            placeholder = { Text(labelPlaceholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        SectionHeader(stringResource(R.string.event_color))
        ColorGrid(
            colors = palette.colors,
            selected = colorIndex,
            onSelect = onColorChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SectionHeader(stringResource(R.string.event_category))
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                val selected = category.id == selectedCategoryId
                FilterChip(
                    selected = selected,
                    onClick = { onCategoryChange(category.id) },
                    label = { Text(category.displayName()) },
                    leadingIcon = if (selected) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null,
                )
            }
            AssistChip(
                onClick = { showNewCategory = true },
                label = { Text(stringResource(R.string.category_new)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        }
    }

    if (showNewCategory) {
        TextInputDialog(
            title = stringResource(R.string.category_new),
            initial = "",
            label = stringResource(R.string.category_name),
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = onCreateCategory,
            onDismiss = { showNewCategory = false },
        )
    }
}

@Composable
fun rememberRandomPlaceholder(): String {
    val examples = androidx.compose.ui.res.stringArrayResource(R.array.event_label_examples)
    val index = rememberSaveable { examples.indices.random() }
    return examples.getOrElse(index) { "" }
}

