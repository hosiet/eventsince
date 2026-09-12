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
package me.byang.eventsince.core.color

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContrastTest {
    @Test
    fun `bright backgrounds get black text`() {
        assertEquals(Contrast.BLACK, Contrast.textColorFor(Contrast.parseHex("#FFF794")!!))
        assertEquals(Contrast.BLACK, Contrast.textColorFor(Contrast.WHITE))
    }

    @Test
    fun `dark backgrounds get white text`() {
        assertEquals(Contrast.WHITE, Contrast.textColorFor(Contrast.parseHex("#008CB7")!!))
        assertEquals(Contrast.WHITE, Contrast.textColorFor(Contrast.parseHex("#BB0000")!!))
        assertEquals(Contrast.WHITE, Contrast.textColorFor(Contrast.BLACK))
    }

    @Test
    fun `threshold is 200`() {
        // (200*299 + 200*587 + 200*114)/1000 == 200 -> black
        assertEquals(Contrast.BLACK, Contrast.textColorFor(Contrast.parseHex("#C8C8C8")!!))
        assertEquals(Contrast.WHITE, Contrast.textColorFor(Contrast.parseHex("#C7C7C7")!!))
    }

    @Test
    fun `hex round trip`() {
        assertEquals("#FFA229", Contrast.toHex(Contrast.parseHex("#ffa229")!!))
        assertEquals(0xFF112233.toInt(), Contrast.parseHex("FF112233"))
        assertNull(Contrast.parseHex("nope"))
        assertNull(Contrast.parseHex(null))
    }
}
