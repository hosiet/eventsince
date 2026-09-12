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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.EventService
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.ui.navigation.EventFormatRoute
import javax.inject.Inject

@HiltViewModel
class EventFormatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EventRepository,
    preferences: PreferencesRepository,
    private val service: EventService,
) : ViewModel() {

    val eventId: String = savedStateHandle.toRoute<EventFormatRoute>().eventId

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    val globalFormat: StateFlow<Set<DurationUnit>> = preferences.preferences.map { it.format }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DurationUnit.DEFAULT)

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    init {
        viewModelScope.launch { _event.value = repository.getEvent(eventId) }
    }

    fun save(format: Set<DurationUnit>?) {
        viewModelScope.launch {
            service.setEventFormat(eventId, format)
            _done.value = true
        }
    }
}
