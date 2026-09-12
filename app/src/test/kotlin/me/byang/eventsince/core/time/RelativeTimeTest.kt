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
package me.byang.eventsince.core.time

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RelativeTimeTest {
    @Test
    fun `formats days hours minutes`() {
        val now = 1_700_000_000_000L
        val then = now - (3 * 86_400_000L + 4 * 3_600_000L + 12 * 60_000L + 30_000L)
        assertEquals("3d 4h 12min", RelativeTime.format(now, then))
    }

    @Test
    fun `omits zero parts`() {
        val now = 1_700_000_000_000L
        assertEquals("2d 5min", RelativeTime.format(now, now - (2 * 86_400_000L + 5 * 60_000L)))
        assertEquals("7h", RelativeTime.format(now, now - 7 * 3_600_000L))
    }

    @Test
    fun `below a minute is now`() {
        val now = 1_700_000_000_000L
        assertNull(RelativeTime.format(now, now - 59_000L))
        assertNull(RelativeTime.format(now, now + 60_000L))
    }
}
