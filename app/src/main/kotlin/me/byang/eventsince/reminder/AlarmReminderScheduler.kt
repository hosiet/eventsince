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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.Clock
import me.byang.eventsince.domain.ReminderScheduler
import me.byang.eventsince.domain.model.Event
import me.byang.eventsince.domain.model.Reminder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exact alarms via [AlarmManager]. Each reminder gets its own [PendingIntent], identified by
 * the intent data `eventsince://reminder/{id}` so that cancelling only needs the id.
 */
@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: EventRepository,
    private val clock: Clock,
) : ReminderScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override suspend fun rescheduleEvent(eventId: String) {
        val reminders = repository.getReminders(eventId)
        reminders.forEach { cancel(it.id) }
        val event = repository.getEvent(eventId) ?: return
        if (!event.isRunning || event.isArchived) return
        reminders.forEach { schedule(event, it) }
    }

    override suspend fun cancelEvent(eventId: String) {
        repository.getReminders(eventId).forEach { cancel(it.id) }
    }

    override suspend fun cancelReminder(reminderId: String) = cancel(reminderId)

    override suspend fun rescheduleAll() {
        val events = repository.getActiveEvents().associateBy { it.id }
        repository.getAllReminders().forEach { reminder ->
            cancel(reminder.id)
            val event = events[reminder.eventId] ?: return@forEach
            if (event.isRunning) schedule(event, reminder)
        }
    }

    private fun schedule(event: Event, reminder: Reminder) {
        val fireAt = reminder.fireAt(event.startAt)
        if (fireAt <= clock.now()) return
        val pi = pendingIntent(reminder.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        // USE_EXACT_ALARM is granted at install time; fall back to an inexact alarm just in case.
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }
    }

    private fun cancel(reminderId: String) {
        val pi = pendingIntent(reminderId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(reminderId: String, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .setData(Uri.parse("eventsince://reminder/$reminderId"))
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }
}
