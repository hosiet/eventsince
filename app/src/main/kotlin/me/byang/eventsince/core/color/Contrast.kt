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

/** Foreground colour selection for a coloured background, using YIQ luminance. */
object Contrast {
    const val BLACK: Int = 0xFF000000.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()

    /** YIQ luminance in 0..255. */
    fun yiq(argb: Int): Int {
        val r = argb shr 16 and 0xFF
        val g = argb shr 8 and 0xFF
        val b = argb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    /** Black text when the background is bright (YIQ >= 200), white otherwise. */
    fun textColorFor(argb: Int): Int = if (yiq(argb) >= 200) BLACK else WHITE

    fun toHex(argb: Int): String = String.format("#%06X", argb and 0xFFFFFF)

    /** Parses `#RRGGBB` or `#AARRGGBB`; returns null if malformed. */
    fun parseHex(hex: String?): Int? {
        val s = hex?.trim()?.removePrefix("#") ?: return null
        return when (s.length) {
            6 -> s.toLongOrNull(16)?.let { (0xFF000000L or it).toInt() }
            8 -> s.toLongOrNull(16)?.toInt()
            else -> null
        }
    }
}
