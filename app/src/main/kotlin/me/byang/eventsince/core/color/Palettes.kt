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

/**
 * A palette of 15 event colours, laid out as 5 columns x 3 rows (light / medium / dark).
 * [swatch] is the representative colour shown in the theme picker.
 */
data class EventPalette(val key: String, val colors: List<Int>, val swatch: Int) {
    init { require(colors.size == SIZE) { "palette $key must have $SIZE colours" } }

    fun color(index: Int): Int = colors[index.coerceIn(0, SIZE - 1)]

    companion object {
        const val SIZE = 15

        fun of(key: String, swatchIndex: Int, vararg hex: String): EventPalette {
            val colors = hex.map { requireNotNull(Contrast.parseHex(it)) { "bad colour $it" } }
            return EventPalette(key, colors, colors[swatchIndex])
        }
    }
}

object Palettes {
    const val DEFAULT_KEY = "default"
    /** Pseudo palette derived from Material You system colours at runtime. */
    const val SYSTEM_KEY = "system"

    val DEFAULT = EventPalette.of(
        DEFAULT_KEY, 10,
        "#A8E6F9", "#B2FF96", "#FFF794", "#FFD193", "#FFBBBB",
        "#26B9E7", "#73DD4D", "#FFED00", "#FFA229", "#EE4848",
        "#008CB7", "#2CA700", "#E6C700", "#D87B00", "#BB0000",
    )
    val UNICORN = EventPalette.of(
        "unicorn", 6,
        "#ACF1FF", "#9CC1FF", "#E0ACFF", "#FCD1FF", "#FFB8E0",
        "#77E9FF", "#4A8EFF", "#C156FF", "#F47DFC", "#FF7CC7",
        "#04ABCC", "#004FD4", "#8B00DA", "#C200CE", "#D10077",
    )
    val SUNRISE = EventPalette.of(
        "sunrise", 12,
        "#FFC2C2", "#FFBCE0", "#FCD6BD", "#FFEBCA", "#FFFBC2",
        "#FF7C7C", "#FF6EBB", "#FFC29A", "#FFD58D", "#FFF78A",
        "#FF0000", "#FF0088", "#FF6600", "#FFA000", "#FFED00",
    )
    val OCEAN = EventPalette.of(
        "ocean", 12,
        "#D7BDE7", "#B7AEDA", "#AFC2E2", "#B5D5E2", "#C3EBF1",
        "#A965D1", "#7B65D2", "#6A97E2", "#5CB4DA", "#66DFF1",
        "#7F00CA", "#2B01D0", "#0055E4", "#0099DA", "#00C3E1",
    )
    val FOREST = EventPalette.of(
        "forest", 10,
        "#BDDBB8", "#BCF2C9", "#B2F8E5", "#A5F3FF", "#98C5FF",
        "#78A971", "#66E684", "#2BEEB9", "#00DCFF", "#4395FF",
        "#107A00", "#02C02F", "#00C791", "#00ABC6", "#005FDA",
    )
    val AUTUMN = EventPalette.of(
        "autumn", 10,
        "#8C7A7A", "#FFA6A6", "#FFBE86", "#FECE7D", "#FFF4AC",
        "#824A4A", "#FF5959", "#FFA14F", "#FFA000", "#FFED76",
        "#7A0000", "#D00101", "#E46A00", "#DA8800", "#F2D200",
    )
    val WINTER = EventPalette.of(
        "winter", 9,
        "#D7E3FC", "#8FBDD5", "#8EB9EF", "#C3A7F5", "#B2B8FF",
        "#C1D3FE", "#4692BA", "#3F75BA", "#814ED8", "#414EF8",
        "#8DAEFE", "#0077B6", "#023E8A", "#4000AE", "#000FD5",
    )
    val CANDY_SHOP = EventPalette.of(
        "candyShop", 10,
        "#E4CCFF", "#FFCCEB", "#FFF2A1", "#C6F1FF", "#B7FFF5",
        "#BC8DF5", "#FFA5DB", "#FFED7A", "#74DCFF", "#6BFFEB",
        "#9B5DE5", "#F15BB5", "#FEE440", "#00BBF9", "#00F5D4",
    )
    val MONOCHROME = EventPalette.of(
        "monoChrome", 12,
        "#F5F5F5", "#ECECEC", "#DBDBDB", "#D2D2D2", "#C4C4C4",
        "#C2C2C2", "#B0B0B0", "#A3A3A3", "#909090", "#747474",
        "#676767", "#585858", "#444444", "#313131", "#000000",
    )

    val BUILT_IN: List<EventPalette> = listOf(
        DEFAULT, UNICORN, SUNRISE, OCEAN, FOREST, AUTUMN, WINTER, CANDY_SHOP, MONOCHROME,
    )

    fun byKey(key: String?): EventPalette? = BUILT_IN.firstOrNull { it.key == key }
}
