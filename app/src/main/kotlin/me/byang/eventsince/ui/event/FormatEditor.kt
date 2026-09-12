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
package me.byang.eventsince.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.byang.eventsince.R
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.core.time.ElapsedFormatter
import me.byang.eventsince.ui.common.rememberUnitSuffixes

@Composable
fun DurationUnit.displayName(): String = stringResource(
    when (this) {
        DurationUnit.YEARS -> R.string.unit_years
        DurationUnit.MONTHS -> R.string.unit_months
        DurationUnit.WEEKS -> R.string.unit_weeks
        DurationUnit.DAYS -> R.string.unit_days
        DurationUnit.HOURS -> R.string.unit_hours
        DurationUnit.MINUTES -> R.string.unit_minutes
        DurationUnit.SECONDS -> R.string.unit_seconds
    },
)

/** Seven unit toggles; at least one unit always stays selected. Shows a live preview. */
@Composable
fun FormatEditor(
    units: Set<DurationUnit>,
    onChange: (Set<DurationUnit>) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val suffixes = rememberUnitSuffixes()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DurationUnit.entries.forEach { unit ->
                val selected = unit in units
                FilterChip(
                    selected = selected,
                    enabled = enabled,
                    onClick = {
                        val next = if (selected) units - unit else units + unit
                        if (next.isNotEmpty()) onChange(next)
                    },
                    label = { Text(unit.displayName()) },
                    leadingIcon = if (selected) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null,
                )
            }
        }
        val sample = 1_700_000_000_000L
        val sampleEnd = sample + 400L * 86_400_000L + 5 * 3_600_000L + 7 * 60_000L + 9_000L
        Text(
            text = stringResource(R.string.format_preview, ElapsedFormatter.format(sample, sampleEnd, units, suffixes)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (DurationUnit.MINUTES in units || DurationUnit.SECONDS in units) {
            Text(
                text = stringResource(R.string.format_widget_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
