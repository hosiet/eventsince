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
package me.byang.eventsince.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.byang.eventsince.R
import me.byang.eventsince.core.color.Contrast
import me.byang.eventsince.core.color.EventPalette
import me.byang.eventsince.core.color.Palettes
import me.byang.eventsince.data.prefs.CategoryHeaderMode
import me.byang.eventsince.data.prefs.DarkMode
import me.byang.eventsince.ui.common.SectionHeader
import me.byang.eventsince.ui.theme.EventPaletteResolver

@Composable
fun AppearanceScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val palettes = remember { Palettes.BUILT_IN + EventPaletteResolver.systemPalette(context) }

    Scaffold(topBar = { SettingsTopBar(stringResource(R.string.appearance_title), onBack) }) { padding ->
        val p = prefs ?: return@Scaffold
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            SectionHeader(stringResource(R.string.appearance_mode))
            RadioRow(stringResource(R.string.appearance_mode_system), p.darkMode == DarkMode.SYSTEM) { viewModel.setDarkMode(DarkMode.SYSTEM) }
            RadioRow(stringResource(R.string.appearance_mode_light), p.darkMode == DarkMode.LIGHT) { viewModel.setDarkMode(DarkMode.LIGHT) }
            RadioRow(stringResource(R.string.appearance_mode_dark), p.darkMode == DarkMode.DARK) { viewModel.setDarkMode(DarkMode.DARK) }

            ListItem(
                headlineContent = { Text(stringResource(R.string.appearance_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.appearance_dynamic_color_hint)) },
                trailingContent = { Switch(checked = p.dynamicColor, onCheckedChange = viewModel::setDynamicColor) },
                modifier = Modifier.clickable { viewModel.setDynamicColor(!p.dynamicColor) },
            )

            SectionHeader(stringResource(R.string.appearance_palette))
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                palettes.forEach { palette ->
                    PaletteSwatch(palette, selected = palette.key == p.paletteKey) { viewModel.setPalette(palette.key) }
                }
            }

            SectionHeader(stringResource(R.string.appearance_headers), Modifier.padding(top = 16.dp))
            RadioRow(
                stringResource(R.string.appearance_headers_auto), p.categoryHeaders == CategoryHeaderMode.AUTO,
                supporting = stringResource(R.string.appearance_headers_auto_hint),
            ) { viewModel.setCategoryHeaders(CategoryHeaderMode.AUTO) }
            RadioRow(stringResource(R.string.appearance_headers_always), p.categoryHeaders == CategoryHeaderMode.ALWAYS) {
                viewModel.setCategoryHeaders(CategoryHeaderMode.ALWAYS)
            }
        }
    }
}

@Composable
private fun PaletteSwatch(palette: EventPalette, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                listOf(0 until 5, 5 until 10, 10 until 15).forEach { row ->
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        row.forEach { i -> Box(Modifier.weight(1f).fillMaxSize().background(Color(palette.color(i)))) }
                    }
                }
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(Contrast.textColorFor(palette.swatch)),
                    modifier = Modifier.align(Alignment.Center).background(Color(palette.swatch), RoundedCornerShape(50)).padding(2.dp),
                )
            }
        }
        Text(paletteName(palette.key), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun paletteName(key: String): String = stringResource(
    when (key) {
        "default" -> R.string.palette_default
        "unicorn" -> R.string.palette_unicorn
        "sunrise" -> R.string.palette_sunrise
        "ocean" -> R.string.palette_ocean
        "forest" -> R.string.palette_forest
        "autumn" -> R.string.palette_autumn
        "winter" -> R.string.palette_winter
        "candyShop" -> R.string.palette_candy_shop
        "monoChrome" -> R.string.palette_monochrome
        Palettes.SYSTEM_KEY -> R.string.palette_system
        else -> R.string.palette_default
    },
)
