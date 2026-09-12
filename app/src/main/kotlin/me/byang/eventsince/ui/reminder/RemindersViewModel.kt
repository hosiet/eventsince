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
package me.byang.eventsince.ui.reminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.EventService
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit
import me.byang.eventsince.ui.navigation.RemindersRoute
import javax.inject.Inject

data class RemindersUiState(
    val loaded: Boolean = false,
    val event: Event? = null,
    val reminders: List<Reminder> = emptyList(),
    val dateFormat: DateFormatPreference = DateFormatPreference.SYSTEM,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EventRepository,
    preferences: PreferencesRepository,
    private val service: EventService,
) : ViewModel() {

    val eventId: String = savedStateHandle.toRoute<RemindersRoute>().eventId

    val uiState: StateFlow<RemindersUiState> = combine(
        repository.observeEvent(eventId),
        repository.observeReminders(eventId),
        preferences.preferences,
    ) { event, reminders, prefs ->
        RemindersUiState(loaded = true, event = event, reminders = reminders, dateFormat = prefs.dateFormat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    /** Returns false when the platform limit of pending alarms would be exceeded. */
    fun add(target: Int, unit: ReminderUnit, onLimitReached: () -> Unit) {
        if (target <= 0) return
        viewModelScope.launch {
            if (repository.countReminders() >= MAX_REMINDERS) {
                onLimitReached()
            } else {
                service.addReminder(eventId, target, unit)
            }
        }
    }

    fun delete(id: String) = viewModelScope.launch { service.deleteReminder(id) }

    companion object {
        const val MAX_REMINDERS = 500
    }
}
