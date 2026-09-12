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
package me.byang.eventsince.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable data class AddEventRoute(val categoryId: String? = null)
@Serializable data class EventDetailRoute(val eventId: String)
@Serializable data class EditEventRoute(val eventId: String)
@Serializable data class EventFormatRoute(val eventId: String)
@Serializable data class HistoryRoute(val eventId: String)
@Serializable data class RemindersRoute(val eventId: String)
@Serializable data class ShareRoute(val eventId: String)
@Serializable object CategoriesRoute
@Serializable object SettingsRoute
@Serializable object AppearanceRoute
@Serializable object FormatsRoute
@Serializable object LanguageRoute
@Serializable object BackupRoute
@Serializable object CloudBackupRoute
