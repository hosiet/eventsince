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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import me.byang.eventsince.R

/** Rounded capsule showing an elapsed duration on an event colour. */
/**
 * Coloured block showing an elapsed-time string.
 *
 * The text shrinks from [fontSize] down to [minFontSize] before it is allowed to wrap, and it
 * only wraps at all when [maxLines] is greater than one. Digits use tabular figures so the
 * block does not shift each second.
 */
@Composable
fun DurationChip(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 12.sp,
    maxLines: Int = 1,
    horizontalPadding: Int = 12,
    verticalPadding: Int = 6,
) {
    val style = LocalTextStyle.current.copy(
        color = foreground,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        fontFeatureSettings = "tnum",
    )
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
        contentAlignment = Alignment.Center,
    ) {
        val maxWidth = constraints.maxWidth
        // Wrap only when even the smallest font size overflows a single line.
        val fitsOneLine = maxLines <= 1 || maxWidth == Constraints.Infinity || remember(text, style, minFontSize, maxWidth) {
            !textMeasurer.measure(
                text = text,
                style = style.copy(fontSize = minFontSize),
                maxLines = 1,
                softWrap = false,
                constraints = Constraints(maxWidth = maxWidth),
            ).didOverflowWidth
        }
        Text(
            text = text,
            // The inherited line height suits body text; wrapped lines need one that scales with the font.
            style = if (fitsOneLine) style else style.copy(lineHeight = 1.2.em),
            autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = fontSize),
            maxLines = if (fitsOneLine) 1 else maxLines,
        )
    }
}

/** 5 x 3 grid of colour swatches; [selected] shows a check mark. */
@Composable
fun ColorGrid(
    colors: List<Color>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier.heightIn(max = 240.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(4.dp),
        userScrollEnabled = false,
    ) {
        items(colors.size) { index ->
            val color = colors[index]
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(color, CircleShape)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (index == selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(me.byang.eventsince.core.color.Contrast.textColorFor(color.toArgbInt())),
                    )
                }
            }
        }
    }
}

fun Color.toArgbInt(): Int = toArgb()

@Composable
fun ConfirmDialog(
    title: String,
    text: String?,
    confirmLabel: String = stringResource(R.string.action_confirm),
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    label: String,
    confirmLabel: String = stringResource(R.string.action_save),
    allowBlank: Boolean = false,
    singleLine: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    singleLine = singleLine,
                    modifier = Modifier.fillMaxWidth(),
                )
                extraContent?.invoke()
            }
        },
        confirmButton = {
            TextButton(
                enabled = allowBlank || value.isNotBlank(),
                onClick = { onConfirm(value); onDismiss() },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** A titled group of rows in a settings-like list. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
