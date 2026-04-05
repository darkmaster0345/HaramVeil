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

import android.content.BroadcastReceiver
import android.content.Context
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.UserManager
import androidx.work.WorkManager
import com.haramveil.accessibility.isHaramVeilAccessibilityServiceEnabled
import com.haramveil.data.local.StatsCleanupWorker
import com.haramveil.overlay.VeilOverlayController
import com.haramveil.utils.RootAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HaramVeilForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var bootstrapStore: ProtectionBootstrapStore
    private var heartbeatJob: Job? = null
    private var unlockedRuntimeStarted = false
    private var userUnlockedReceiver: BroadcastReceiver? = null
    private var intentionalStopRequested = false

    override fun onCreate() {
        super.onCreate()
        bootstrapStore = ProtectionBootstrapStore(applicationContext)
        ProtectionNotificationHelper.ensureChannels(applicationContext)
        registerUserUnlockedReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ActionStop) {
            intentionalStopRequested = true
            stopRuntime(cancelRecovery = true)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!bootstrapStore.isBootstrapReady() || !bootstrapStore.isProtectionEnabled()) {
            intentionalStopRequested = true
            stopRuntime(cancelRecovery = true)
            stopSelf()
            return START_NOT_STICKY
        }

        intentionalStopRequested = false
        startForeground(
            ProtectionNotificationHelper.ForegroundNotificationId,
            ProtectionNotificationHelper.buildForegroundNotification(applicationContext),
        )
        bootstrapStore.markServiceRunning(true)
        bootstrapStore.updateServiceHeartbeat()
        startHeartbeatLoop()
        ServiceHealthAlarmReceiver.schedule(applicationContext)

        if (isUserUnlocked()) {
            enterUnlockedRuntimeIfNeeded()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        unregisterUserUnlockedReceiver()
        stopRuntime(cancelRecovery = intentionalStopRequested)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startHeartbeatLoop() {
        if (heartbeatJob?.isActive == true) {
            return
        }
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                bootstrapStore.updateServiceHeartbeat()
                delay(HeartbeatIntervalMs)
            }
        }
    }

    private fun enterUnlockedRuntimeIfNeeded() {
        if (unlockedRuntimeStarted) {
            return
        }
        unlockedRuntimeStarted = true

        VeilOverlayController.start(applicationContext)
        ProtectionHealthWorker.enqueue(applicationContext)
        StatsCleanupWorker.enqueue(applicationContext)

        val rootModeEnabled = RootAccessHelper.isRootAvailable()
        if (rootModeEnabled) {
            RootAccessHelper.tryLowerNiceValue()
            if (!isHaramVeilAccessibilityServiceEnabled(applicationContext)) {
                RootAccessHelper.tryRestoreAccessibilityService(applicationContext)
            }
        }

        if (!isHaramVeilAccessibilityServiceEnabled(applicationContext)) {
            ProtectionNotificationHelper.showAccessibilityReenablePrompt(applicationContext)
        }
    }

    private fun stopRuntime(
        cancelRecovery: Boolean,
    ) {
        heartbeatJob?.cancel()
        heartbeatJob = null
        bootstrapStore.markServiceRunning(false)
        if (cancelRecovery) {
            ServiceHealthAlarmReceiver.cancel(applicationContext)
            WorkManager.getInstance(applicationContext)
                .cancelUniqueWork(ProtectionHealthWorker.UniqueWorkName)
            WorkManager.getInstance(applicationContext)
                .cancelUniqueWork(StatsCleanupWorker.UniqueWorkName)
        }
        applicationContext.stopService(Intent(applicationContext, com.haramveil.overlay.VeilOverlayService::class.java))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun isUserUnlocked(): Boolean {
        val userManager = getSystemService(UserManager::class.java) ?: return true
        return userManager.isUserUnlocked
    }

    private fun registerUserUnlockedReceiver() {
        if (userUnlockedReceiver != null) {
            return
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_UNLOCKED &&
                    bootstrapStore.isBootstrapReady() &&
                    bootstrapStore.isProtectionEnabled()
                ) {
                    enterUnlockedRuntimeIfNeeded()
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_USER_UNLOCKED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
        userUnlockedReceiver = receiver
    }

    private fun unregisterUserUnlockedReceiver() {
        val receiver = userUnlockedReceiver ?: return
        runCatching {
            unregisterReceiver(receiver)
        }
        userUnlockedReceiver = null
    }

    companion object {
        const val ActionStart = "com.haramveil.service.action.START"
        const val ActionStop = "com.haramveil.service.action.STOP"
        const val HeartbeatIntervalMs = 30_000L
    }
}
