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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.haramveil.MainActivity
import com.haramveil.R
import com.haramveil.security.DeviceAdminController

object ProtectionNotificationHelper {
    const val ForegroundNotificationId = 9101
    private const val DeviceAdminNotificationId = 9102
    private const val AccessibilityNotificationId = 9103

    private const val ForegroundChannelId = "haramveil_protection_runtime"
    private const val AlertsChannelId = "haramveil_protection_alerts"

    fun ensureChannels(
        context: Context,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        if (notificationManager.getNotificationChannel(ForegroundChannelId) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    ForegroundChannelId,
                    "HaramVeil Protection",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Keeps HaramVeil active in the background so monitoring can resume quickly."
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                },
            )
        }
        if (notificationManager.getNotificationChannel(AlertsChannelId) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    AlertsChannelId,
                    "HaramVeil Alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Warns you when accessibility or tamper protection needs attention."
                    enableVibration(true)
                },
            )
        }
    }

    fun buildForegroundNotification(
        context: Context,
    ): Notification =
        NotificationCompat.Builder(context, ForegroundChannelId)
            .setSmallIcon(R.drawable.ic_haramveil_shield)
            .setContentTitle("Protection Active")
            .setContentText("Guarding your eyes")
            .setContentIntent(appLaunchPendingIntent(context))
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

    fun showDeviceAdminDisabled(
        context: Context,
    ) {
        ensureChannels(context)
        notify(
            context = context,
            notificationId = DeviceAdminNotificationId,
            notification = NotificationCompat.Builder(context, AlertsChannelId)
                .setSmallIcon(R.drawable.ic_haramveil_shield)
                .setContentTitle("HaramVeil protection has been disabled")
                .setContentText("Tap to re-enable Device Admin and restore tamper protection.")
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        DeviceAdminNotificationId,
                        DeviceAdminController.buildEnableIntent(context),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build(),
        )
    }

    fun showAccessibilityReenablePrompt(
        context: Context,
    ) {
        ensureChannels(context)
        notify(
            context = context,
            notificationId = AccessibilityNotificationId,
            notification = NotificationCompat.Builder(context, AlertsChannelId)
                .setSmallIcon(R.drawable.ic_haramveil_shield)
                .setContentTitle("Accessibility attention needed")
                .setContentText("HaramVeil needs Accessibility Service active to monitor risky content.")
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        AccessibilityNotificationId,
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build(),
        )
    }

    private fun notify(
        context: Context,
        notificationId: Int,
        notification: Notification,
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        notificationManager.notify(notificationId, notification)
    }

    private fun appLaunchPendingIntent(
        context: Context,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            ForegroundNotificationId,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
