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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY position")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): CategoryEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM categories")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)

    @Query("UPDATE categories SET sort = :sort WHERE id = :id")
    suspend fun updateSort(id: String, sort: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE archived_at IS NULL ORDER BY position")
    fun observeActive(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE archived_at IS NOT NULL ORDER BY archived_at DESC")
    fun observeArchived(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE archived_at IS NULL ORDER BY position")
    suspend fun getActive(): List<EventEntity>

    @Query("SELECT * FROM events WHERE category_id = :categoryId AND archived_at IS NULL ORDER BY position")
    suspend fun getActiveInCategory(categoryId: String): List<EventEntity>

    @Query("SELECT COUNT(*) FROM events WHERE category_id = :categoryId AND archived_at IS NULL")
    suspend fun countActiveInCategory(categoryId: String): Int

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeById(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Query("SELECT * FROM events ORDER BY position")
    suspend fun getAllIncludingArchived(): List<EventEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM events WHERE category_id = :categoryId")
    suspend fun maxPositionInCategory(categoryId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Query("UPDATE events SET category_id = :categoryId, position = :position WHERE id = :id")
    suspend fun updatePlacement(id: String, categoryId: String, position: Int)

    @Query("UPDATE events SET category_id = :toCategoryId WHERE category_id = :fromCategoryId")
    suspend fun moveAll(fromCategoryId: String, toCategoryId: String)

    @Query("UPDATE events SET format = :format WHERE archived_at IS NULL")
    suspend fun setFormatForAll(format: String?)

    @Query("UPDATE events SET archived_at = :archivedAt WHERE id = :id")
    suspend fun setArchivedAt(id: String, archivedAt: Long?)

    /** Removes the event for good; logs and reminders go with it (cascade). */
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM events WHERE category_id = :categoryId AND archived_at IS NULL")
    suspend fun deleteActiveInCategory(categoryId: String)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_logs WHERE event_id = :eventId ORDER BY at DESC")
    fun observeForEvent(eventId: String): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_logs WHERE event_id = :eventId ORDER BY at DESC")
    suspend fun getForEvent(eventId: String): List<EventLogEntity>

    @Query("SELECT * FROM event_logs ORDER BY at")
    suspend fun getAll(): List<EventLogEntity>

    @Query("SELECT * FROM event_logs WHERE id = :id")
    suspend fun getById(id: String): EventLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: EventLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<EventLogEntity>)

    @Query("UPDATE event_logs SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String?)

    @Query("DELETE FROM event_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM event_logs WHERE event_id = :eventId AND type = :type")
    suspend fun deleteByType(eventId: String, type: String)

    @Query("DELETE FROM event_logs")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE event_id = :eventId ORDER BY created_at")
    fun observeForEvent(eventId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE event_id = :eventId ORDER BY created_at")
    suspend fun getForEvent(eventId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reminders WHERE event_id = :eventId")
    suspend fun deleteForEvent(eventId: String)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
