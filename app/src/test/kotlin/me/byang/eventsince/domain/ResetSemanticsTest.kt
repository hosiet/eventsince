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

import me.byang.eventsince.domain.model.Event
import org.junit.Test
import kotlin.test.assertEquals

class ResetSemanticsTest {
    private val now = 1_700_000_000_000L
    private val hour = 3_600_000L

    private fun event(startAt: Long, stoppedAt: Long? = null) = Event(
        id = "e", categoryId = "c", label = "x", startAt = startAt, stoppedAt = stoppedAt,
        colorIndex = 0, createdAt = startAt, format = null, position = 0,
    )

    @Test
    fun `reset now ends the running period at now`() {
        val r = ResetSemantics.reset(event(now - 5 * hour), requestedStartAt = null, now = now)
        assertEquals(now, r.newStartAt)
        assertEquals(now - 5 * hour, r.previousStartAt)
        assertEquals(5 * hour, r.elapsedMs)
    }

    @Test
    fun `reset from a past date uses that date as the new start`() {
        val r = ResetSemantics.reset(event(now - 5 * hour), requestedStartAt = now - 2 * hour, now = now)
        assertEquals(now - 2 * hour, r.newStartAt)
        assertEquals(5 * hour, r.elapsedMs)
    }

    @Test
    fun `future dates are ignored`() {
        val r = ResetSemantics.reset(event(now - hour), requestedStartAt = now + hour, now = now)
        assertEquals(now, r.newStartAt)
        assertEquals(now, ResetSemantics.clampStart(now + 1, now))
        assertEquals(now - 1, ResetSemantics.clampStart(now - 1, now))
        assertEquals(now, ResetSemantics.clampStart(null, now))
    }

    @Test
    fun `stopped events measure up to the stop instant`() {
        val r = ResetSemantics.reset(event(now - 10 * hour, stoppedAt = now - 4 * hour), null, now)
        assertEquals(6 * hour, r.elapsedMs)
        assertEquals(now, r.newStartAt)
    }

    @Test
    fun `stop elapsed`() {
        assertEquals(3 * hour, ResetSemantics.stopElapsed(event(now - 3 * hour), now))
        assertEquals(0L, ResetSemantics.stopElapsed(event(now + hour), now))
    }
}
