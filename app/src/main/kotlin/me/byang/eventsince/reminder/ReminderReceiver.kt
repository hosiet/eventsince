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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.domain.ReminderScheduler
import javax.inject.Inject

/** Fires a reminder notification, and re-schedules everything after boot or clock changes. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: EventRepository
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_FIRE -> fire(context, intent.data?.lastPathSegment)
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    -> scheduler.rescheduleAll()
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(context: Context, reminderId: String?) {
        if (reminderId == null) return
        val reminder = repository.getReminder(reminderId) ?: return
        val event = repository.getEvent(reminder.eventId) ?: return
        if (!event.isRunning || event.isArchived) return
        ReminderNotifications.show(context, event, reminder)
    }

    companion object {
        const val ACTION_FIRE = "me.byang.eventsince.action.FIRE_REMINDER"
    }
}
