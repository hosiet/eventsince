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
package me.byang.eventsince.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.byang.eventsince.R
import me.byang.eventsince.data.prefs.PreferencesRepository
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.ui.common.displayLabel
import me.byang.eventsince.ui.common.displayName
import me.byang.eventsince.ui.navigation.DEEP_LINK_ADD
import me.byang.eventsince.ui.theme.EventSinceTheme
import me.byang.eventsince.ui.theme.LocalEventPalette
import javax.inject.Inject

/** Shown when a widget is added: pick the event it should display. */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var repository: EventRepository
    @Inject lateinit var preferences: PreferencesRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish(); return
        }

        setContent {
            val prefs by preferences.preferences.collectAsStateWithLifecycle(null)
            val groupsFlow = remember { repository.observeHome().map { g -> g.filter { it.events.isNotEmpty() } } }
            val groups by groupsFlow.collectAsStateWithLifecycle(emptyList())
            val p = prefs ?: return@setContent
            EventSinceTheme(darkMode = p.darkMode, dynamicColor = p.dynamicColor, paletteKey = p.paletteKey) {
                val palette = LocalEventPalette.current
                Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_pick_event)) }) }) { padding ->
                    if (groups.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            TextButton(onClick = {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_ADD)).setPackage(packageName))
                                finish()
                            }) { Text(stringResource(R.string.widget_no_events)) }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                            groups.forEach { group ->
                                item {
                                    Text(
                                        group.category.displayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                                    )
                                }
                                items(group.events, key = { it.id }) { event ->
                                    ListItem(
                                        headlineContent = { Text(event.displayLabel()) },
                                        leadingContent = {
                                            Box(Modifier.size(24.dp).background(palette.background(event.colorIndex), CircleShape))
                                        },
                                        modifier = Modifier.clickable { bind(appWidgetId, event.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bind(appWidgetId: Int, eventId: String) {
        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@WidgetConfigActivity)
            val glanceId = manager.getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply { this[EventWidget.EVENT_ID] = eventId }
            }
            val provider = AppWidgetManager.getInstance(this@WidgetConfigActivity).getAppWidgetInfo(appWidgetId)?.provider?.className
            val style = if (provider == ResetWidgetReceiver::class.java.name) WidgetStyle.RESET else WidgetStyle.STANDARD
            EventWidget(style).update(this@WidgetConfigActivity, glanceId)
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}
