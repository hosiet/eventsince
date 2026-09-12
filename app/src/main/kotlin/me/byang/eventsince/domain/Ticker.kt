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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits "now" immediately and then on every wall-clock boundary of [intervalMillis]. */
fun ticker(intervalMillis: Long, clock: Clock = Clock.SYSTEM): Flow<Long> = flow {
    while (true) {
        val now = clock.now()
        emit(now)
        val wait = intervalMillis - now % intervalMillis
        delay(if (wait <= 0) intervalMillis else wait)
    }
}
