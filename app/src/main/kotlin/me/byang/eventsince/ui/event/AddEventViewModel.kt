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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.EventService
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.ui.navigation.AddEventRoute
import javax.inject.Inject

@HiltViewModel
class AddEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: EventRepository,
    private val service: EventService,
) : ViewModel() {

    val initialCategoryId: String? = savedStateHandle.toRoute<AddEventRoute>().categoryId

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    private val _createdCategoryId = MutableStateFlow<String?>(null)
    val createdCategoryId: StateFlow<String?> = _createdCategoryId.asStateFlow()

    fun create(label: String, colorIndex: Int, categoryId: String?, startAt: Long?) {
        viewModelScope.launch {
            service.createEvent(label, colorIndex, categoryId, startAt)
            _done.value = true
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch { _createdCategoryId.value = service.createCategory(name).id }
    }
}
