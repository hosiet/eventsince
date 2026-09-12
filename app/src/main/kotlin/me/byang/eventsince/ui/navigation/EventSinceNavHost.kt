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

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import me.byang.eventsince.ui.backup.BackupScreen
import me.byang.eventsince.ui.category.CategoriesScreen
import me.byang.eventsince.ui.cloud.CloudBackupScreen
import me.byang.eventsince.ui.event.AddEventScreen
import me.byang.eventsince.ui.event.EditEventScreen
import me.byang.eventsince.ui.event.EventDetailScreen
import me.byang.eventsince.ui.event.EventFormatScreen
import me.byang.eventsince.ui.history.HistoryScreen
import me.byang.eventsince.ui.home.HomeScreen
import me.byang.eventsince.ui.reminder.RemindersScreen
import me.byang.eventsince.ui.settings.AppearanceScreen
import me.byang.eventsince.ui.settings.FormatsScreen
import me.byang.eventsince.ui.settings.LanguageScreen
import me.byang.eventsince.ui.settings.SettingsScreen
import me.byang.eventsince.ui.share.ShareScreen

const val DEEP_LINK_EVENT = "eventsince://event"
const val DEEP_LINK_ADD = "eventsince://add"

@Composable
fun EventSinceNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onAddEvent = { navController.navigate(AddEventRoute()) },
                onOpenEvent = { navController.navigate(EventDetailRoute(it)) },
                onOpenCategories = { navController.navigate(CategoriesRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<AddEventRoute>(deepLinks = listOf(navDeepLink<AddEventRoute>(basePath = DEEP_LINK_ADD))) {
            AddEventScreen(onDone = { navController.popBackStackOrHome() })
        }
        composable<EventDetailRoute>(deepLinks = listOf(navDeepLink<EventDetailRoute>(basePath = DEEP_LINK_EVENT))) {
            EventDetailScreen(
                onBack = { navController.popBackStackOrHome() },
                onEdit = { navController.navigate(EditEventRoute(it)) },
                onFormat = { navController.navigate(EventFormatRoute(it)) },
                onHistory = { navController.navigate(HistoryRoute(it)) },
                onReminders = { navController.navigate(RemindersRoute(it)) },
                onShare = { navController.navigate(ShareRoute(it)) },
            )
        }
        composable<EditEventRoute> {
            EditEventScreen(onDone = { navController.popBackStack() })
        }
        composable<EventFormatRoute> {
            EventFormatScreen(onDone = { navController.popBackStack() })
        }
        composable<ShareRoute> {
            ShareScreen(onBack = { navController.popBackStack() })
        }
        composable<HistoryRoute> {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable<RemindersRoute> {
            RemindersScreen(onBack = { navController.popBackStack() })
        }
        composable<AppearanceRoute> {
            AppearanceScreen(onBack = { navController.popBackStack() })
        }
        composable<FormatsRoute> {
            FormatsScreen(onBack = { navController.popBackStack() })
        }
        composable<LanguageRoute> {
            LanguageScreen(onBack = { navController.popBackStack() })
        }
        composable<CloudBackupRoute> {
            CloudBackupScreen(onBack = { navController.popBackStack() })
        }
        composable<BackupRoute> {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable<CategoriesRoute> {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenCategories = { navController.navigate(CategoriesRoute) },
                onOpenAppearance = { navController.navigate(AppearanceRoute) },
                onOpenFormats = { navController.navigate(FormatsRoute) },
                onOpenLanguage = { navController.navigate(LanguageRoute) },
                onOpenBackup = { navController.navigate(BackupRoute) },
                onOpenCloudBackup = { navController.navigate(CloudBackupRoute) },
            )
        }
    }
}

/** Pops, or falls back to Home when the destination was reached through a deep link. */
fun NavHostController.popBackStackOrHome() {
    if (!popBackStack()) navigate(HomeRoute) { popUpTo(0) }
}
