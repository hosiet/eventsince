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
package me.byang.eventsince.ui.theme

import android.content.Context
import me.byang.eventsince.core.color.EventPalette
import me.byang.eventsince.core.color.Palettes

/** Resolves a palette key to 15 ARGB colours, including the Material You based "system" palette. */
object EventPaletteResolver {

    fun resolve(context: Context, key: String?): EventPalette = when (key) {
        Palettes.SYSTEM_KEY -> systemPalette(context)
        else -> Palettes.byKey(key) ?: Palettes.DEFAULT
    }

    /**
     * Light / medium / dark rows taken from the three Material You accent tonal palettes,
     * five tones per row so that every row spans accent1, accent2 and accent3.
     */
    fun systemPalette(context: Context): EventPalette {
        val ids = listOf(
            android.R.color.system_accent1_100, android.R.color.system_accent2_100, android.R.color.system_accent3_100,
            android.R.color.system_accent1_200, android.R.color.system_accent3_200,
            android.R.color.system_accent1_400, android.R.color.system_accent2_400, android.R.color.system_accent3_400,
            android.R.color.system_accent1_500, android.R.color.system_accent3_500,
            android.R.color.system_accent1_700, android.R.color.system_accent2_700, android.R.color.system_accent3_700,
            android.R.color.system_accent1_800, android.R.color.system_accent3_800,
        )
        val colors = ids.map { context.getColor(it) or 0xFF000000.toInt() }
        return EventPalette(Palettes.SYSTEM_KEY, colors, colors[10])
    }
}
