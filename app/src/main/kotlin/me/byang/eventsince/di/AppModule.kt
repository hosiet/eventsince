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
package me.byang.eventsince.di

import android.content.ContentValues
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.byang.eventsince.R
import me.byang.eventsince.data.local.EventSinceDatabase
import me.byang.eventsince.data.repository.EventRepository
import me.byang.eventsince.data.repository.RoomEventRepository
import me.byang.eventsince.domain.Clock
import me.byang.eventsince.domain.ReminderScheduler
import me.byang.eventsince.reminder.AlarmReminderScheduler
import java.util.UUID
import javax.inject.Singleton

private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EventSinceDatabase =
        Room.databaseBuilder(context, EventSinceDatabase::class.java, "eventsince.db")
            .addCallback(SeedCallback(context.getString(R.string.sample_event_label)))
            .build()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.preferencesStore

    @Provides
    fun provideClock(): Clock = Clock.SYSTEM

    /** First-launch content: one default category holding a sample event that started "now". */
    private class SeedCallback(private val sampleLabel: String) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val categoryId = UUID.randomUUID().toString()
            val eventId = UUID.randomUUID().toString()
            db.insert("categories", 0, ContentValues().apply {
                put("id", categoryId); put("name", ""); put("position", 0); put("sort", "ASC"); put("is_default", 1)
            })
            db.insert("events", 0, ContentValues().apply {
                put("id", eventId); put("category_id", categoryId); put("label", sampleLabel)
                put("start_at", now); putNull("stopped_at"); put("color_index", 0); put("created_at", now)
                putNull("format"); put("position", 0); putNull("archived_at")
            })
            db.insert("event_logs", 0, ContentValues().apply {
                put("id", UUID.randomUUID().toString()); put("event_id", eventId); put("type", "CREATED"); put("at", now)
                putNull("previous_start_at"); put("new_start_at", now); putNull("elapsed_ms")
                putNull("label_snapshot"); putNull("color_hex"); putNull("notes"); putNull("payload")
            })
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun bindEventRepository(impl: RoomEventRepository): EventRepository

    @Binds
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler
}
