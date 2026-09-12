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

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, EventEntity::class, EventLogEntity::class, ReminderEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = EventSinceDatabase.ArchiveMigration::class)],
)
abstract class EventSinceDatabase : RoomDatabase() {
    /** Version 2: soft deletion became archiving, so the column and the log type are renamed. */
    @RenameColumn(tableName = "events", fromColumnName = "deleted_at", toColumnName = "archived_at")
    class ArchiveMigration : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE event_logs SET type = 'ARCHIVED' WHERE type = 'DELETED'")
        }
    }

    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun reminderDao(): ReminderDao
}
