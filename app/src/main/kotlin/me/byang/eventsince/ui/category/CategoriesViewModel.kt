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
package me.byang.eventsince.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.EventService
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.CategoryDeletion
import javax.inject.Inject

data class CategoryRow(val category: Category, val eventCount: Int)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    repository: EventRepository,
    private val service: EventService,
) : ViewModel() {

    val rows: StateFlow<List<CategoryRow>> = repository.observeHome()
        .map { groups -> groups.map { CategoryRow(it.category, it.events.size) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) = viewModelScope.launch { service.createCategory(name) }
    fun rename(id: String, name: String) = viewModelScope.launch { service.renameCategory(id, name) }
    fun reorder(orderedIds: List<String>) = viewModelScope.launch { service.reorderCategories(orderedIds) }
    fun delete(id: String, deletion: CategoryDeletion) = viewModelScope.launch { service.deleteCategory(id, deletion) }
}
