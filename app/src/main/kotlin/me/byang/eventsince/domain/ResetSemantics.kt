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

import me.byang.eventsince.domain.model.Event

/**
 * Pure computation of what a reset (or a creation) does, so the rules can be tested
 * without a database.
 */
object ResetSemantics {

    data class ResetResult(
        /** Start of the new period. */
        val newStartAt: Long,
        /** Start of the period that just ended. */
        val previousStartAt: Long,
        /** Length of the period that just ended. */
        val elapsedMs: Long,
    )

    /**
     * A requested start instant is honoured only if it lies in the past; otherwise "now" is used.
     */
    fun clampStart(requestedStartAt: Long?, now: Long): Long =
        if (requestedStartAt != null && requestedStartAt < now) requestedStartAt else now

    /**
     * Ends the event's current period. A stopped event's period ends at its stop instant,
     * a running one at [now]. The new period starts at [requestedStartAt] if that is in the past.
     */
    fun reset(event: Event, requestedStartAt: Long?, now: Long): ResetResult {
        val newStart = clampStart(requestedStartAt, now)
        val periodEnd = event.stoppedAt ?: now
        val elapsed = (periodEnd - event.startAt).coerceAtLeast(0)
        return ResetResult(newStartAt = newStart, previousStartAt = event.startAt, elapsedMs = elapsed)
    }

    /** Elapsed time of the current period when stopping now. */
    fun stopElapsed(event: Event, now: Long): Long = (now - event.startAt).coerceAtLeast(0)
}
