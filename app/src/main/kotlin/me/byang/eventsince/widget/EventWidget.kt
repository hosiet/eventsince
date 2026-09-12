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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import me.byang.eventsince.R
import me.byang.eventsince.core.color.Contrast
import me.byang.eventsince.core.time.DurationUnit
import me.byang.eventsince.core.time.ElapsedFormatter
import me.byang.eventsince.core.time.UnitSuffixes
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.ui.theme.EventPaletteResolver

enum class WidgetStyle { STANDARD, RESET }

/**
 * Home-screen widget showing one event: coloured background, elapsed time and label.
 * Minutes and seconds are dropped from the format because widgets refresh only every few minutes.
 */
class EventWidget(private val style: WidgetStyle) : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    /** Everything the widget needs to draw one event, loaded off the composition. */
    private data class WidgetData(val event: Event?, val elapsed: String?, val background: Int)

    private suspend fun load(context: Context, eventId: String?): WidgetData {
        val entryPoint = WidgetEntryPoint.get(context)
        val event = eventId?.let { entryPoint.repository().getEvent(it) }
        val prefs = entryPoint.preferences().current()
        val palette = EventPaletteResolver.resolve(context, prefs.paletteKey)
        val format = widgetFormat(event?.effectiveFormat(prefs.format) ?: prefs.format)
        val suffixes = UnitSuffixes(
            context.getString(R.string.unit_suffix_years), context.getString(R.string.unit_suffix_months),
            context.getString(R.string.unit_suffix_weeks), context.getString(R.string.unit_suffix_days),
            context.getString(R.string.unit_suffix_hours), context.getString(R.string.unit_suffix_minutes),
            context.getString(R.string.unit_suffix_seconds),
        )
        val background = event?.let { palette.color(it.colorIndex) } ?: 0xFF444444.toInt()
        val elapsed = event?.let { ElapsedFormatter.format(it.startAt, it.endAt(System.currentTimeMillis()), format, suffixes) }
        return WidgetData(event, elapsed, background)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The composition stays alive for the session, so the bound event id is read from the
        // live state inside it (the configuration activity writes it after the first render).
        provideContent {
            val eventId = currentState<Preferences>()[EVENT_ID]
            val data by produceState<WidgetData?>(initialValue = null, eventId) { value = load(context, eventId) }
            GlanceTheme {
                val d = data
                if (d != null) {
                    WidgetContent(event = d.event, elapsed = d.elapsed, background = d.background, style = style, hasEventId = eventId != null)
                } else {
                    Box(GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF444444))).cornerRadius(16.dp)) {}
                }
            }
        }
    }

    @Composable
    private fun WidgetContent(event: Event?, elapsed: String?, background: Int, style: WidgetStyle, hasEventId: Boolean) {
        val context = LocalContext.current
        val fg = Contrast.textColorFor(background)
        val alpha = if (event == null || !event.isRunning) 0.55f else 1f
        val textColor = ColorProvider(Color(fg).copy(alpha = alpha))
        val openIntent = event?.let {
            Intent(Intent.ACTION_VIEW, Uri.parse("eventsince://event/${it.id}")).setPackage(context.packageName)
        }
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(background)))
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .let { if (openIntent != null) it.clickable(actionStartActivity(openIntent)) else it },
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (style == WidgetStyle.STANDARD) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_stat_event),
                        contentDescription = null,
                        modifier = GlanceModifier.size(36.dp),
                        colorFilter = androidx.glance.ColorFilter.tint(textColor),
                    )
                    Spacer(GlanceModifier.width(10.dp))
                }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    when {
                        event == null && hasEventId -> Text(
                            context.getString(R.string.widget_event_deleted),
                            style = TextStyle(color = textColor, fontSize = 14.sp),
                        )
                        event == null -> Text(
                            context.getString(R.string.widget_not_configured),
                            style = TextStyle(color = textColor, fontSize = 14.sp),
                        )
                        else -> {
                            Text(elapsed.orEmpty(), style = TextStyle(color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                            Text(
                                event.label.ifBlank { context.getString(R.string.event_untitled) },
                                style = TextStyle(color = textColor, fontSize = 13.sp),
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (style == WidgetStyle.RESET && event != null) {
                    Box(
                        modifier = GlanceModifier
                            .size(40.dp)
                            .cornerRadius(20.dp)
                            .background(ColorProvider(Color(fg).copy(alpha = 0.18f)))
                            .clickable(actionRunCallback<ResetEventAction>(actionParametersOf(ResetEventAction.EVENT_ID to event.id))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_reset),
                            contentDescription = context.getString(R.string.event_reset_now),
                            modifier = GlanceModifier.size(24.dp),
                            colorFilter = androidx.glance.ColorFilter.tint(textColor),
                        )
                    }
                }
            }
        }
    }

    companion object {
        val EVENT_ID = stringPreferencesKey("event_id")

        /** Widgets cannot tick, so minutes and seconds are removed; days and hours are the fallback. */
        fun widgetFormat(units: Set<DurationUnit>): Set<DurationUnit> {
            val coarse = units - DurationUnit.MINUTES - DurationUnit.SECONDS
            return coarse.ifEmpty { setOf(DurationUnit.DAYS, DurationUnit.HOURS) }
        }

        suspend fun updateAll(context: Context) {
            EventWidget(WidgetStyle.STANDARD).updateAll(context)
            EventWidget(WidgetStyle.RESET).updateAll(context)
        }
    }
}

/** The reset button of the "reset" widget style. */
class ResetEventAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = parameters[EVENT_ID] ?: return
        val entryPoint = WidgetEntryPoint.get(context)
        val prefs = entryPoint.preferences().current()
        val event = entryPoint.repository().getEvent(eventId) ?: return
        val palette = EventPaletteResolver.resolve(context, prefs.paletteKey)
        entryPoint.eventService().resetEvent(eventId, null, Contrast.toHex(palette.color(event.colorIndex)))
        EventWidget.updateAll(context)
    }

    companion object {
        val EVENT_ID = ActionParameters.Key<String>("event_id")
    }
}

