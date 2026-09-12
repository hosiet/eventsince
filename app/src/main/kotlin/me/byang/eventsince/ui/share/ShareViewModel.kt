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
package me.byang.eventsince.ui.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.ui.navigation.ShareRoute
import javax.inject.Inject

data class ShareUiState(val event: Event? = null, val format: Set<DurationUnit> = DurationUnit.DEFAULT)

@HiltViewModel
class ShareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: EventRepository,
    preferences: PreferencesRepository,
) : ViewModel() {
    val eventId: String = savedStateHandle.toRoute<ShareRoute>().eventId

    val uiState: StateFlow<ShareUiState> = combine(repository.observeEvent(eventId), preferences.preferences) { event, prefs ->
        ShareUiState(event, event?.effectiveFormat(prefs.format) ?: prefs.format)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShareUiState())
}
