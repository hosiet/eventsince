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
package me.byang.eventsince.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.byang.eventsince.R
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.data.prefs.WeekStart
import me.byang.eventsince.ui.common.ConfirmDialog
import me.byang.eventsince.ui.common.SectionHeader
import me.byang.eventsince.ui.common.formatDateTime
import me.byang.eventsince.ui.event.FormatEditor

@Composable
fun FormatsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmApply by rememberSaveable { mutableStateOf(false) }
    val applied = stringResource(R.string.formats_applied)

    Scaffold(
        topBar = { SettingsTopBar(stringResource(R.string.formats_title), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val p = prefs ?: return@Scaffold
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            SectionHeader(stringResource(R.string.formats_duration))
            FormatEditor(units = p.format, onChange = viewModel::setFormat)
            OutlinedButton(
                onClick = { confirmApply = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text(stringResource(R.string.formats_apply_all)) }

            SectionHeader(stringResource(R.string.formats_date), Modifier.padding(top = 16.dp))
            Text(
                stringResource(R.string.formats_date_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            val now = System.currentTimeMillis()
            RadioRow(
                stringResource(R.string.formats_date_system), p.dateFormat == DateFormatPreference.SYSTEM,
                supporting = formatDateTime(now, DateFormatPreference.SYSTEM),
            ) { viewModel.setDateFormat(DateFormatPreference.SYSTEM) }
            RadioRow("yyyy-MM-dd", p.dateFormat == DateFormatPreference.ISO, supporting = formatDateTime(now, DateFormatPreference.ISO)) {
                viewModel.setDateFormat(DateFormatPreference.ISO)
            }
            RadioRow("dd/MM/yyyy", p.dateFormat == DateFormatPreference.DAY_MONTH_YEAR, supporting = formatDateTime(now, DateFormatPreference.DAY_MONTH_YEAR)) {
                viewModel.setDateFormat(DateFormatPreference.DAY_MONTH_YEAR)
            }
            RadioRow("MM/dd/yyyy", p.dateFormat == DateFormatPreference.MONTH_DAY_YEAR, supporting = formatDateTime(now, DateFormatPreference.MONTH_DAY_YEAR)) {
                viewModel.setDateFormat(DateFormatPreference.MONTH_DAY_YEAR)
            }

            SectionHeader(stringResource(R.string.formats_week_start), Modifier.padding(top = 16.dp))
            RadioRow(stringResource(R.string.formats_week_start_system), p.weekStart == WeekStart.SYSTEM) { viewModel.setWeekStart(WeekStart.SYSTEM) }
            RadioRow(stringResource(R.string.formats_week_start_sunday), p.weekStart == WeekStart.SUNDAY) { viewModel.setWeekStart(WeekStart.SUNDAY) }
            RadioRow(stringResource(R.string.formats_week_start_monday), p.weekStart == WeekStart.MONDAY) { viewModel.setWeekStart(WeekStart.MONDAY) }
        }
    }

    if (confirmApply) {
        ConfirmDialog(
            title = stringResource(R.string.formats_apply_all),
            text = stringResource(R.string.formats_apply_all_confirm),
            onConfirm = {
                viewModel.applyFormatToAllEvents()
                scope.launch { snackbar.showSnackbar(applied) }
            },
            onDismiss = { confirmApply = false },
        )
    }
}
