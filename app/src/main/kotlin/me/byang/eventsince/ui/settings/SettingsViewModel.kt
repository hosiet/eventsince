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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.prefs.CategoryHeaderMode
import me.byang.eventsince.data.prefs.DarkMode
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.prefs.UserPreferences
import me.byang.eventsince.data.prefs.WeekStart
import me.byang.eventsince.domain.EventService
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val service: EventService,
) : ViewModel() {

    val prefs: StateFlow<UserPreferences?> = preferences.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDarkMode(mode: DarkMode) = viewModelScope.launch { preferences.setDarkMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { preferences.setDynamicColor(enabled) }
    fun setPalette(key: String) = viewModelScope.launch { preferences.setPalette(key) }
    fun setCategoryHeaders(mode: CategoryHeaderMode) = viewModelScope.launch { preferences.setCategoryHeaders(mode) }
    fun setFormat(units: Set<DurationUnit>) = viewModelScope.launch { preferences.setFormat(units) }
    fun setDateFormat(pref: DateFormatPreference) = viewModelScope.launch { preferences.setDateFormat(pref) }
    fun setWeekStart(start: WeekStart) = viewModelScope.launch { preferences.setWeekStart(start) }

    fun applyFormatToAllEvents() = viewModelScope.launch {
        service.applyFormatToAllEvents(preferences.current().format)
    }
}
