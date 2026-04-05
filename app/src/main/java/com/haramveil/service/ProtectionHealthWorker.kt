/*
 * HaramVeil
 * Copyright (C) 2026 HaramVeil Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.haramveil.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.haramveil.accessibility.isHaramVeilAccessibilityServiceEnabled
import com.haramveil.utils.RootAccessHelper
import java.util.concurrent.TimeUnit

class ProtectionHealthWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val bootstrapStore = ProtectionBootstrapStore(context)
        if (!bootstrapStore.isBootstrapReady() || !bootstrapStore.isProtectionEnabled()) {
            return Result.success()
        }

        if (!bootstrapStore.isServiceAlive()) {
            HaramVeilProtectionController.start(context)
        }

        if (!isHaramVeilAccessibilityServiceEnabled(context)) {
            if (RootAccessHelper.isRootAvailable()) {
                RootAccessHelper.tryRestoreAccessibilityService(context)
            }
            if (!isHaramVeilAccessibilityServiceEnabled(context)) {
                ProtectionNotificationHelper.showAccessibilityReenablePrompt(context)
            }
        }

        return Result.success()
    }

    companion object {
        const val UniqueWorkName = "haramveil_protection_health"

        fun enqueue(
            context: Context,
        ) {
            val request = PeriodicWorkRequestBuilder<ProtectionHealthWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UniqueWorkName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
        }
    }
}
