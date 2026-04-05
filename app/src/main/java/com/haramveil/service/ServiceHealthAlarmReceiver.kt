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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class ServiceHealthAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ActionServiceHealthCheck) {
            return
        }

        val bootstrapStore = ProtectionBootstrapStore(context.applicationContext)
        if (!bootstrapStore.isBootstrapReady() || !bootstrapStore.isProtectionEnabled()) {
            return
        }
        if (!bootstrapStore.isServiceAlive()) {
            HaramVeilProtectionController.start(context.applicationContext)
        }
    }

    companion object {
        const val ActionServiceHealthCheck = "com.haramveil.service.ACTION_SERVICE_HEALTH_CHECK"
        const val HealthCheckIntervalMs = 15 * 60 * 1_000L
        private const val RequestCode = 4109

        fun schedule(
            context: Context,
        ) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + HealthCheckIntervalMs,
                HealthCheckIntervalMs,
                pendingIntent(context),
            )
        }

        fun cancel(
            context: Context,
        ) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            alarmManager.cancel(pendingIntent(context))
        }

        private fun pendingIntent(
            context: Context,
        ): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                RequestCode,
                Intent(context, ServiceHealthAlarmReceiver::class.java).apply {
                    action = ActionServiceHealthCheck
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
