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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.EventService
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.ui.navigation.EventDetailRoute
import javax.inject.Inject

data class EventDetailUiState(
    val loaded: Boolean = false,
    val event: Event? = null,
    val category: Category? = null,
    val globalFormat: Set<DurationUnit> = DurationUnit.DEFAULT,
) {
    val format: Set<DurationUnit> get() = event?.effectiveFormat(globalFormat) ?: globalFormat
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: EventRepository,
    preferences: PreferencesRepository,
    private val service: EventService,
) : ViewModel() {

    val eventId: String = savedStateHandle.toRoute<EventDetailRoute>().eventId

    val uiState: StateFlow<EventDetailUiState> = combine(
        repository.observeEvent(eventId),
        repository.observeCategories(),
        preferences.preferences,
    ) { event, categories, prefs ->
        EventDetailUiState(
            loaded = true,
            event = event,
            category = categories.firstOrNull { it.id == event?.categoryId },
            globalFormat = prefs.format,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailUiState())

    /** True once the event left this screen for good (archived or deleted); the screen then closes. */
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    fun reset(requestedStartAt: Long?, colorHex: String) {
        viewModelScope.launch { service.resetEvent(eventId, requestedStartAt, colorHex) }
    }

    fun stop() {
        viewModelScope.launch { service.stopEvent(eventId) }
    }

    fun archive() {
        viewModelScope.launch {
            service.archiveEvent(eventId)
            _closed.value = true
        }
    }

    fun unarchive() {
        viewModelScope.launch { service.unarchiveEvent(eventId) }
    }

    /** Permanent deletion; there is no way back. */
    fun delete() {
        viewModelScope.launch {
            service.deleteEvent(eventId)
            _closed.value = true
        }
    }
}
