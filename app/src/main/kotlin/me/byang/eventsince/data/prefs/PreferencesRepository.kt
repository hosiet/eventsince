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
package me.byang.eventsince.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.core.time.DurationUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PALETTE = stringPreferencesKey("palette")
        val FORMAT = stringSetPreferencesKey("format")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val WEEK_START = stringPreferencesKey("week_start")
        val CATEGORY_HEADERS = stringPreferencesKey("category_headers")
        val SEEDED = booleanPreferencesKey("seeded")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { it.toUserPreferences() }

    suspend fun current(): UserPreferences = preferences.first()

    private fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
        darkMode = DarkMode.fromKey(this[Keys.DARK_MODE]),
        dynamicColor = this[Keys.DYNAMIC_COLOR] ?: true,
        paletteKey = this[Keys.PALETTE] ?: UserPreferences().paletteKey,
        format = DurationUnit.parse(this[Keys.FORMAT]),
        dateFormat = DateFormatPreference.fromKey(this[Keys.DATE_FORMAT]),
        weekStart = WeekStart.fromKey(this[Keys.WEEK_START]),
        categoryHeaders = CategoryHeaderMode.fromKey(this[Keys.CATEGORY_HEADERS]),
        seeded = this[Keys.SEEDED] ?: false,
    )

    suspend fun setDarkMode(mode: DarkMode) = dataStore.edit { it[Keys.DARK_MODE] = mode.key }
    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setPalette(key: String) = dataStore.edit { it[Keys.PALETTE] = key }
    suspend fun setFormat(units: Set<DurationUnit>) = dataStore.edit { it[Keys.FORMAT] = DurationUnit.toKeys(units).toSet() }
    suspend fun setDateFormat(pref: DateFormatPreference) = dataStore.edit { it[Keys.DATE_FORMAT] = pref.key }
    suspend fun setWeekStart(start: WeekStart) = dataStore.edit { it[Keys.WEEK_START] = start.key }
    suspend fun setCategoryHeaders(mode: CategoryHeaderMode) = dataStore.edit { it[Keys.CATEGORY_HEADERS] = mode.key }
    suspend fun setSeeded() = dataStore.edit { it[Keys.SEEDED] = true }

    /** Applies preferences restored from a backup. */
    suspend fun restore(prefs: UserPreferences) = dataStore.edit {
        it[Keys.DARK_MODE] = prefs.darkMode.key
        it[Keys.DYNAMIC_COLOR] = prefs.dynamicColor
        it[Keys.PALETTE] = prefs.paletteKey
        it[Keys.FORMAT] = DurationUnit.toKeys(prefs.format).toSet()
        it[Keys.DATE_FORMAT] = prefs.dateFormat.key
        it[Keys.WEEK_START] = prefs.weekStart.key
        it[Keys.CATEGORY_HEADERS] = prefs.categoryHeaders.key
    }
}
