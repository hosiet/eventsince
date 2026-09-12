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
package me.byang.eventsince.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.data.prefs.CategoryHeaderMode
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.EventService
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.CategoryWithEvents
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.EventPlacement
import javax.inject.Inject

/** One row of the flat, draggable home list. */
sealed interface HomeRow {
    val key: String

    data class Header(val category: Category) : HomeRow {
        override val key: String get() = "c:${category.id}"
    }

    data class Item(val event: Event) : HomeRow {
        override val key: String get() = "e:${event.id}"
    }

    /** The expandable "archived events" row that closes the list. */
    data class ArchiveHeader(val count: Int, val expanded: Boolean) : HomeRow {
        override val key: String get() = "archive"
    }

    data class Archived(val event: Event) : HomeRow {
        override val key: String get() = "a:${event.id}"
    }

    val isArchive: Boolean get() = this is ArchiveHeader || this is Archived
}

data class HomeUiState(
    val loaded: Boolean = false,
    val groups: List<CategoryWithEvents> = emptyList(),
    val showHeaders: Boolean = false,
    val globalFormat: Set<DurationUnit> = DurationUnit.DEFAULT,
    val query: String = "",
    val archived: List<Event> = emptyList(),
    val archivedExpanded: Boolean = false,
) {
    val isFiltering: Boolean get() = query.isNotBlank()
    val rows: List<HomeRow> = buildList {
        groups.forEach { group ->
            if (showHeaders) add(HomeRow.Header(group.category))
            group.events.forEach { add(HomeRow.Item(it)) }
        }
        if (archived.isNotEmpty()) {
            add(HomeRow.ArchiveHeader(archived.size, archivedExpanded))
            if (archivedExpanded) archived.forEach { add(HomeRow.Archived(it)) }
        }
    }
    val isEmpty: Boolean get() = loaded && groups.all { it.events.isEmpty() } && archived.isEmpty()
    val hasSeconds: Boolean
        get() {
            val shown = groups.flatMap { it.events } + if (archivedExpanded) archived else emptyList()
            return shown.any { it.isRunning && DurationUnit.SECONDS in it.effectiveFormat(globalFormat) }
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: EventRepository,
    preferences: PreferencesRepository,
    private val service: EventService,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val archivedExpanded = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeHome(), repository.observeArchived(), preferences.preferences, query, archivedExpanded,
    ) { groups, archived, prefs, q, expanded ->
        val needle = q.trim()
        fun matches(event: Event) = needle.isEmpty() || event.label.contains(needle, ignoreCase = true)
        val visible = if (needle.isEmpty()) groups else groups
            .map { g -> g.copy(events = g.events.filter(::matches)) }
            .filter { it.events.isNotEmpty() }
        HomeUiState(
            loaded = true,
            groups = visible,
            showHeaders = prefs.categoryHeaders == CategoryHeaderMode.ALWAYS || groups.size > 1,
            globalFormat = prefs.format,
            query = q,
            archived = archived.filter(::matches),
            // A search should not hide matches behind a collapsed row.
            archivedExpanded = expanded || needle.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setQuery(value: String) { query.value = value }

    fun toggleArchived() { archivedExpanded.value = !archivedExpanded.value }

    fun toggleSort(categoryId: String) {
        viewModelScope.launch { service.toggleCategorySort(categoryId) }
    }

    /** Persists the order produced by drag-and-drop. */
    fun applyLayout(rows: List<HomeRow>) {
        val state = uiState.value
        val categoryOrder = mutableListOf<String>()
        val placements = mutableListOf<EventPlacement>()
        var current: String? = if (state.showHeaders) null else state.groups.firstOrNull()?.category?.id
        var position = 0
        rows.forEach { row ->
            when (row) {
                is HomeRow.Header -> {
                    categoryOrder += row.category.id
                    current = row.category.id
                    position = 0
                }
                is HomeRow.Item -> {
                    val categoryId = current ?: return@forEach
                    placements += EventPlacement(row.event.id, categoryId, position++)
                }
                is HomeRow.ArchiveHeader, is HomeRow.Archived -> Unit
            }
        }
        // Categories without a visible header keep their relative order after the visible ones.
        state.groups.map { it.category.id }.forEach { if (it !in categoryOrder) categoryOrder += it }
        viewModelScope.launch { service.applyHomeLayout(categoryOrder, placements) }
    }
}
