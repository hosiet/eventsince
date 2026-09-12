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
package me.byang.eventsince.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import me.byang.eventsince.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Two-step picker: a date (1941 .. today), then an optional time of day (default 00:00).
 * Cancelling the time step keeps 00:00; cancelling the date step dismisses without result.
 * The result is clamped to [maxMillis] by the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDateTimePicker(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onPicked: (Long) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    if (pickedDate == null) {
        val initialUtc = remember(initialMillis) {
            val date = initialMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: today
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val todayUtcEnd = remember { today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialUtc,
            yearRange = 1941..today.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcEnd
                override fun isSelectableYear(year: Int): Boolean = year in 1941..today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        val utc = state.selectedDateMillis ?: return@TextButton
                        pickedDate = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                    },
                ) { Text(stringResource(R.string.action_next)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = state)
        }
    } else {
        val date = pickedDate!!
        val initialTime = remember(initialMillis) {
            initialMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() } ?: LocalTime.MIDNIGHT
        }
        val timeState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
        fun finish(time: LocalTime) {
            onPicked(date.atTime(time).atZone(zone).toInstant().toEpochMilli())
        }
        AlertDialog(
            onDismissRequest = { finish(LocalTime.MIDNIGHT) },
            title = { Text(stringResource(R.string.picker_time_title)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = { finish(LocalTime.of(timeState.hour, timeState.minute)) }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { finish(LocalTime.MIDNIGHT) }) { Text(stringResource(R.string.picker_time_skip)) }
            },
        )
    }
}
