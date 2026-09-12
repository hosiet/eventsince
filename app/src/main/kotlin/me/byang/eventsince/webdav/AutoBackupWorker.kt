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
package me.byang.eventsince.webdav

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.byang.eventsince.data.prefs.WebDavSettingsRepository
import me.byang.eventsince.domain.DataChangeBus
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val service: CloudBackupService,
    private val settings: WebDavSettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val current = settings.current()
        if (!current.autoBackup || !current.isConfigured) return Result.success()
        return try {
            service.upload()
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "auto-backup"
        private const val MAX_ATTEMPTS = 5
    }
}

/**
 * Listens for data changes and enqueues one debounced upload: every change replaces the
 * pending request, so a burst of edits results in a single upload 30 seconds after the last one.
 */
@Singleton
class AutoBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bus: DataChangeBus,
    private val settings: WebDavSettingsRepository,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            bus.changes.collectLatest { scheduleIfEnabled() }
        }
    }

    suspend fun scheduleIfEnabled() {
        val current = settings.current()
        if (!current.autoBackup || !current.isConfigured) return
        enqueue(current.wifiOnly, delaySeconds = DEBOUNCE_SECONDS)
    }

    fun enqueue(wifiOnly: Boolean, delaySeconds: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(AutoBackupWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(AutoBackupWorker.WORK_NAME)
    }

    companion object {
        const val DEBOUNCE_SECONDS = 30L
    }
}
