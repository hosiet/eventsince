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
package me.byang.eventsince.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import me.byang.eventsince.R
import me.byang.eventsince.core.time.DateFormatPreference
import me.byang.eventsince.core.time.DateFormats
import me.byang.eventsince.core.time.UnitSuffixes
import me.byang.eventsince.domain.model.Category
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.ticker

@Composable
fun rememberUnitSuffixes(): UnitSuffixes {
    val y = stringResource(R.string.unit_suffix_years)
    val mo = stringResource(R.string.unit_suffix_months)
    val w = stringResource(R.string.unit_suffix_weeks)
    val d = stringResource(R.string.unit_suffix_days)
    val h = stringResource(R.string.unit_suffix_hours)
    val mi = stringResource(R.string.unit_suffix_minutes)
    val s = stringResource(R.string.unit_suffix_seconds)
    return remember(y, mo, w, d, h, mi, s) { UnitSuffixes(y, mo, w, d, h, mi, s) }
}

/** Current time, refreshed on every wall-clock boundary of [intervalMillis]. */
@Composable
fun rememberNow(intervalMillis: Long): State<Long> =
    produceState(initialValue = System.currentTimeMillis(), intervalMillis) {
        ticker(intervalMillis).collect { value = it }
    }

@Composable
fun Event.displayLabel(): String = label.ifBlank { stringResource(R.string.event_untitled) }

@Composable
fun Category.displayName(): String =
    name.ifBlank { if (isDefault) stringResource(R.string.category_default_name) else stringResource(R.string.category_unnamed) }

@Composable
fun formatDate(millis: Long, preference: DateFormatPreference): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateFormats.formatDate(millis, preference, locale)
}

@Composable
fun formatDateTime(millis: Long, preference: DateFormatPreference): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateFormats.formatDateTime(millis, preference, locale)
}
