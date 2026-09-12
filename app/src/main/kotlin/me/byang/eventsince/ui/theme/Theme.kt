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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import me.byang.eventsince.core.color.Contrast
import me.byang.eventsince.core.color.EventPalette
import me.byang.eventsince.core.color.Palettes
import me.byang.eventsince.data.prefs.DarkMode

/** The active event palette as Compose colours. */
class EventPaletteColors(val palette: EventPalette) {
    val colors: List<Color> = palette.colors.map { Color(it) }
    fun background(index: Int): Color = Color(palette.color(index))
    fun foreground(index: Int): Color = Color(Contrast.textColorFor(palette.color(index)))
    fun hex(index: Int): String = Contrast.toHex(palette.color(index))
}

val LocalEventPalette = staticCompositionLocalOf { EventPaletteColors(Palettes.DEFAULT) }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1E5F8F),
    secondary = Color(0xFF4F6070),
    tertiary = Color(0xFF6B5778),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9CCAFF),
    secondary = Color(0xFFB7C8DA),
    tertiary = Color(0xFFD6BEE4),
)

@Composable
fun EventSinceTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    dynamicColor: Boolean = true,
    paletteKey: String = Palettes.DEFAULT_KEY,
    content: @Composable () -> Unit,
) {
    val dark = when (darkMode) {
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
        DarkMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && dark -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    val palette = remember(paletteKey, context) { EventPaletteColors(EventPaletteResolver.resolve(context, paletteKey)) }
    CompositionLocalProvider(LocalEventPalette provides palette, LocalIsDarkTheme provides dark) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

val LocalIsDarkTheme = staticCompositionLocalOf { false }
