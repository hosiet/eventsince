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
package me.byang.eventsince.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Emits after any user-visible data mutation (used by widgets and auto backup). */
@Singleton
class DataChangeBus @Inject constructor() {
    private val _changes = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    val changes: SharedFlow<Long> = _changes.asSharedFlow()

    fun notifyChanged() {
        _changes.tryEmit(System.currentTimeMillis())
    }
}
