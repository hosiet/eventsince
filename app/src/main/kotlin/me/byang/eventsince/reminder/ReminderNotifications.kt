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
package me.byang.eventsince.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import me.byang.eventsince.R
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.Reminder
import me.byang.eventsince.domain.model.ReminderUnit

object ReminderNotifications {
    const val CHANNEL_ID = "reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun bodyText(context: Context, reminder: Reminder): String {
        val plural = when (reminder.unit) {
            ReminderUnit.HOURS -> R.plurals.reminder_reached_hours
            ReminderUnit.DAYS -> R.plurals.reminder_reached_days
            ReminderUnit.WEEKS -> R.plurals.reminder_reached_weeks
            ReminderUnit.YEARS -> R.plurals.reminder_reached_years
        }
        return context.resources.getQuantityString(plural, reminder.target, reminder.target)
    }

    fun show(context: Context, event: Event, reminder: Reminder) {
        ensureChannel(context)
        val title = event.label.ifBlank { context.getString(R.string.event_untitled) }
        val open = Intent(Intent.ACTION_VIEW, Uri.parse("eventsince://event/${event.id}"))
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(
            context, reminder.id.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_event)
            .setContentTitle(title)
            .setContentText(bodyText(context, reminder))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            manager.notify(reminder.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked; nothing else to do.
        }
    }
}
