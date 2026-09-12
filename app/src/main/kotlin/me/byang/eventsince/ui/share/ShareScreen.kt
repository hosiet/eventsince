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
package me.byang.eventsince.ui.share

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.byang.eventsince.R
import me.byang.eventsince.core.color.Contrast
import me.byang.eventsince.core.time.ElapsedFormatter
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.ui.common.DurationChip
import me.byang.eventsince.ui.common.displayLabel
import me.byang.eventsince.ui.common.rememberUnitSuffixes
import me.byang.eventsince.ui.theme.LocalEventPalette
import java.io.File

private enum class CardStyle { EVENT_COLOR, LIGHT, DARK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(onBack: () -> Unit, viewModel: ShareViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val layers = CardStyle.entries.map { rememberGraphicsLayer() }
    val shareTitle = stringResource(R.string.share_title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    val event = state.event
                    IconButton(enabled = event != null, onClick = {
                        val e = event ?: return@IconButton
                        scope.launch { shareLayer(context, layers[selected], e, shareTitle) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_send))
                    }
                },
            )
        },
    ) { padding ->
        val event = state.event ?: return@Scaffold
        val suffixes = rememberUnitSuffixes()
        // The duration is computed once when the screen opens and stays frozen.
        val elapsed = remember(event.id, state.format) {
            ElapsedFormatter.format(event.startAt, event.endAt(System.currentTimeMillis()), state.format, suffixes)
        }
        val palette = LocalEventPalette.current
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.share_pick_style), style = MaterialTheme.typography.bodyMedium)
            CardStyle.entries.forEachIndexed { index, style ->
                val (bg, fg) = when (style) {
                    CardStyle.EVENT_COLOR -> palette.background(event.colorIndex) to palette.foreground(event.colorIndex)
                    CardStyle.LIGHT -> Color.White to Color.Black
                    CardStyle.DARK -> Color(0xFF121212) to Color.White
                }
                ShareCard(
                    event = event,
                    elapsed = elapsed,
                    background = bg,
                    foreground = fg,
                    layer = layers[index],
                    selected = selected == index,
                    onClick = { selected = index },
                )
            }
        }
    }
}

@Composable
private fun ShareCard(
    event: Event,
    elapsed: String,
    background: Color,
    foreground: Color,
    layer: GraphicsLayer,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (selected) 3.dp else 1.dp, outline, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_stat_event),
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.app_name), color = foreground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(event.displayLabel(), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            DurationChip(
                text = elapsed,
                background = foreground,
                foreground = if (foreground == Color.White) Color.Black else if (foreground == Color.Black) Color.White else background,
                fontSize = 24.sp,
                horizontalPadding = 18,
                verticalPadding = 10,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

private suspend fun shareLayer(context: Context, layer: GraphicsLayer, event: Event, title: String) {
    val bitmap = layer.toImageBitmap().asAndroidBitmap()
    val safeName = event.label.filter { it.isLetterOrDigit() }.ifBlank { "event" }
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "$safeName.png").also { f ->
            f.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, event.label)
        clipData = android.content.ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
