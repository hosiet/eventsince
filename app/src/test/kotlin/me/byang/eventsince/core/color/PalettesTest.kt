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

class PalettesTest {
    @Test
    fun `nine palettes with fifteen colours each`() {
        assertEquals(9, Palettes.BUILT_IN.size)
        assertEquals(9, Palettes.BUILT_IN.map { it.key }.toSet().size)
        Palettes.BUILT_IN.forEach { assertEquals(15, it.colors.size, it.key) }
    }

    @Test
    fun `lookup by key`() {
        assertEquals("#008CB7", Contrast.toHex(Palettes.byKey("default")!!.color(10)))
        assertEquals("#000000", Contrast.toHex(Palettes.byKey("monoChrome")!!.color(14)))
        assertNull(Palettes.byKey("nope"))
    }

    @Test
    fun `index is clamped`() {
        assertEquals(Palettes.DEFAULT.color(0), Palettes.DEFAULT.color(-3))
        assertEquals(Palettes.DEFAULT.color(14), Palettes.DEFAULT.color(99))
    }
}
