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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.prefs.WeekStart
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.EventService
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventLog
import me.byang.eventsince.domain.model.LogType
import me.byang.eventsince.ui.navigation.HistoryRoute
import javax.inject.Inject

enum class LogFilter(val types: Set<LogType>?) {
    ALL(null),
    RESET(setOf(LogType.RESET)),
    CREATED(setOf(LogType.CREATED)),
    STOPPED(setOf(LogType.STOPPED)),
    EDITED(setOf(LogType.EDITED)),
    OTHER(setOf(LogType.ARCHIVED, LogType.UNARCHIVED, LogType.IMPORTED)),
}

data class HistoryUiState(
    val loaded: Boolean = false,
    val event: Event? = null,
    val logs: List<EventLog> = emptyList(),
    val filter: LogFilter = LogFilter.ALL,
    val format: Set<DurationUnit> = DurationUnit.DEFAULT,
    val dateFormat: DateFormatPreference = DateFormatPreference.SYSTEM,
    val weekStart: WeekStart = WeekStart.SYSTEM,
) {
    val resets: List<EventLog> get() = logs.filter { it.type == LogType.RESET }
    val visibleLogs: List<EventLog> get() = filter.types?.let { t -> logs.filter { it.type in t } } ?: logs
    val longestStreak: EventLog? get() = resets.filter { (it.elapsedMs ?: 0) > 0 }.maxByOrNull { it.elapsedMs ?: 0 }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: EventRepository,
    preferences: PreferencesRepository,
    private val service: EventService,
) : ViewModel() {

    val eventId: String = savedStateHandle.toRoute<HistoryRoute>().eventId

    private val filter = MutableStateFlow(LogFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeEvent(eventId),
        repository.observeLogs(eventId),
        preferences.preferences,
        filter,
    ) { event, logs, prefs, filter ->
        HistoryUiState(
            loaded = true,
            event = event,
            logs = logs,
            filter = filter,
            format = event?.effectiveFormat(prefs.format) ?: prefs.format,
            dateFormat = prefs.dateFormat,
            weekStart = prefs.weekStart,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(value: LogFilter) { filter.value = value }
    fun updateNotes(logId: String, notes: String?) = viewModelScope.launch { service.updateLogNotes(logId, notes) }
    fun deleteLog(logId: String) = viewModelScope.launch { service.deleteLog(logId) }
    fun clearResets() = viewModelScope.launch { service.clearResetLogs(eventId) }
}
