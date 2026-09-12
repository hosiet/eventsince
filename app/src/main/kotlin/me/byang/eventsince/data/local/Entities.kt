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
package me.byang.eventsince.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
    val sort: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("category_id"), Index("archived_at")],
)
data class EventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    val label: String,
    @ColumnInfo(name = "start_at") val startAt: Long,
    @ColumnInfo(name = "stopped_at") val stoppedAt: Long?,
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** JSON array of unit keys, or null to inherit the global format. */
    val format: String?,
    val position: Int,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
)

@Entity(
    tableName = "event_logs",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("event_id"), Index("type"), Index("at")],
)
data class EventLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "event_id") val eventId: String,
    val type: String,
    val at: Long,
    @ColumnInfo(name = "previous_start_at") val previousStartAt: Long?,
    @ColumnInfo(name = "new_start_at") val newStartAt: Long?,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long?,
    @ColumnInfo(name = "label_snapshot") val labelSnapshot: String?,
    @ColumnInfo(name = "color_hex") val colorHex: String?,
    val notes: String?,
    val payload: String?,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("event_id")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "event_id") val eventId: String,
    val target: Int,
    val unit: String,
    val body: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
