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
package me.byang.eventsince.domain

/** Schedules and cancels the OS alarms behind goal reminders. */
interface ReminderScheduler {
    /** Cancels every alarm of the event, then schedules the future ones again if the event is running. */
    suspend fun rescheduleEvent(eventId: String)

    /** Cancels every alarm of the event. */
    suspend fun cancelEvent(eventId: String)

    /** Cancels one alarm. */
    suspend fun cancelReminder(reminderId: String)

    /** Reschedules everything (boot, time change, restore). */
    suspend fun rescheduleAll()
}
