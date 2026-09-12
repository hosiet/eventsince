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

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.byang.eventsince.R
import me.byang.eventsince.ui.common.SectionHeader

private data class LanguageOption(val tag: String?, val label: String)

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val localeManager = remember { context.getSystemService(LocaleManager::class.java) }
    var current by remember { mutableStateOf(localeManager.applicationLocales.toLanguageTags().ifBlank { null }) }
    val options = listOf(
        LanguageOption(null, stringResource(R.string.language_system)),
        LanguageOption("en", "English"),
        LanguageOption("zh-CN", "简体中文"),
    )

    Scaffold(topBar = { SettingsTopBar(stringResource(R.string.language_title), onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionHeader(stringResource(R.string.language_choose))
            options.forEach { option ->
                RadioRow(option.label, selected = current == option.tag) {
                    current = option.tag
                    localeManager.applicationLocales =
                        if (option.tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(option.tag)
                }
            }
            Text(
                stringResource(R.string.language_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
