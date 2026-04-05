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

package com.haramveil.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import com.haramveil.data.local.ProtectionPreferencesRepository
import com.haramveil.data.models.ProtectionSettings
import com.haramveil.detection.mode1.RiskLevel
import com.haramveil.detection.mode2.Mode2Processor
import com.haramveil.detection.mode1.UITreeScanner
import com.haramveil.detection.mode3.Mode3Processor
import com.haramveil.overlay.VeilOverlayController
import com.haramveil.security.AppLockdownManager
import com.haramveil.utils.DetectionBus
import com.haramveil.utils.DetectionTriggerMode
import com.haramveil.utils.DispatcherProvider
import com.haramveil.utils.Mode1WakeRequest
import com.haramveil.utils.ThrottleManager
import com.haramveil.utils.ThrottledScanTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HaramVeilAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dispatcherProvider = DispatcherProvider()
    private val uiTreeScanner = UITreeScanner()
    private lateinit var protectionPreferencesRepository: ProtectionPreferencesRepository
    private lateinit var appLockdownManager: AppLockdownManager
    private val protectionSettingsState = MutableStateFlow(ProtectionSettings())
    private var collectorsStarted = false
    private lateinit var throttleManager: ThrottleManager
    private lateinit var mode2Processor: Mode2Processor
    private lateinit var mode3Processor: Mode3Processor

    override fun onCreate() {
        super.onCreate()
        protectionPreferencesRepository = ProtectionPreferencesRepository(applicationContext)
        appLockdownManager = AppLockdownManager(applicationContext)
        throttleManager = ThrottleManager(
            scope = serviceScope,
            dispatcherProvider = dispatcherProvider,
        ) { pendingEvent ->
            buildScanTrigger(pendingEvent.packageName, pendingEvent.eventType)
        }
        mode2Processor = Mode2Processor(
            context = applicationContext,
            accessibilityServiceProvider = { this },
            protectionPreferencesRepository = protectionPreferencesRepository,
            settingsProvider = { protectionSettingsState.value },
            dispatcherProvider = dispatcherProvider,
        )
        mode3Processor = Mode3Processor(
            context = applicationContext,
            accessibilityServiceProvider = { this },
            protectionPreferencesRepository = protectionPreferencesRepository,
            settingsProvider = { protectionSettingsState.value },
            dispatcherProvider = dispatcherProvider,
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        if (!collectorsStarted) {
            startCollectors()
            collectorsStarted = true
        }
        VeilOverlayController.start(applicationContext)
        Log.i(LogTag, "Accessibility service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val packageName = event.packageName
            ?.toString()
            ?.takeIf(String::isNotBlank)
            ?: rootInActiveWindow?.packageName?.toString()?.takeIf(String::isNotBlank)
            ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            DetectionBus.publishWindowStateObserved(
                packageName = packageName,
                eventType = event.eventType,
            )

            val currentSettings = protectionSettingsState.value
            if (appLockdownManager.isLocked(packageName)) {
                appLockdownManager.lockApp(packageName, currentSettings.lockdownDurationMs)
                DetectionBus.publishVeilRequested(
                    packageName = packageName,
                    triggerMode = DetectionTriggerMode.LOCKDOWN,
                    matchDetails = "App reopen intercepted while its lockdown timer was still active.",
                )
                Log.i(LogTag, "Lockdown re-triggered for $packageName before content scan.")
                return
            }
        }

        val currentSettings = protectionSettingsState.value
        if (!currentSettings.mode1Enabled) {
            return
        }

        if (packageName !in currentSettings.monitoredPackages) {
            return
        }

        throttleManager.submit(
            packageName = packageName,
            eventType = event.eventType,
        )
    }

    override fun onInterrupt() {
        Log.w(LogTag, "Accessibility service interrupted by the system.")
    }

    override fun onDestroy() {
        mode2Processor.destroy()
        mode3Processor.destroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startCollectors() {
        serviceScope.launch(dispatcherProvider.io) {
            protectionPreferencesRepository.settingsFlow.collectLatest { settings ->
                protectionSettingsState.value = settings
                throttleManager.updateDebounceMs(settings.frameSkipIntervalMs)
                if (settings.mode2Enabled) {
                    mode2Processor.start()
                } else {
                    mode2Processor.stop()
                }
                if (settings.mode3Enabled) {
                    mode3Processor.start()
                } else {
                    mode3Processor.stop()
                }
            }
        }

        serviceScope.launch(dispatcherProvider.default) {
            throttleManager.scanTriggers.collectLatest { trigger ->
                trigger ?: return@collectLatest
                processScanTrigger(trigger)
            }
        }
    }

    private suspend fun processScanTrigger(
        trigger: ThrottledScanTrigger,
    ) {
        try {
            val currentSettings = protectionSettingsState.value
            val scanResult = uiTreeScanner.scan(
                rootNode = trigger.rootNode,
                packageName = trigger.packageName,
                keywordBlocklist = currentSettings.keywordBlocklist,
            )

            if (!scanResult.triggered) {
                return
            }

            val wakeMode2 = currentSettings.mode2Enabled
            val wakeMode3 = scanResult.riskLevel == RiskLevel.HIGH && currentSettings.mode3Enabled
            val matchDetails = buildMatchDetails(scanResult)

            DetectionBus.publishMode1Triggered(
                Mode1WakeRequest(
                    scanResult = scanResult,
                    wakeMode2 = wakeMode2,
                    wakeMode3 = wakeMode3,
                ),
            )

            when (scanResult.riskLevel) {
                RiskLevel.MEDIUM -> Unit

                RiskLevel.HIGH -> {
                    if (!currentSettings.mode2Enabled && !currentSettings.mode3Enabled) {
                        DetectionBus.publishVeilRequested(
                            packageName = scanResult.packageName,
                            triggerMode = DetectionTriggerMode.MODE_1,
                            matchDetails = matchDetails,
                        )
                    }
                }

                RiskLevel.LOW -> Unit
            }

            Log.i(
                LogTag,
                "Mode 1 triggered for ${scanResult.packageName} with ${scanResult.riskLevel} risk. $matchDetails",
            )
        } finally {
            trigger.recycle()
        }
    }

    private fun buildScanTrigger(
        packageName: String,
        eventType: Int,
    ): ThrottledScanTrigger? {
        val activeRoot = rootInActiveWindow ?: return null
        val rootSnapshot = AccessibilityNodeInfo.obtain(activeRoot)
        val nodeHash = uiTreeScanner.computeNodeHash(rootSnapshot)
        return ThrottledScanTrigger(
            packageName = packageName,
            eventType = eventType,
            nodeHash = nodeHash,
            rootNode = rootSnapshot,
        )
    }

    private fun buildMatchDetails(
        scanResult: com.haramveil.detection.mode1.ScanResult,
    ): String {
        return scanResult.matchedKeyword?.let { keyword ->
            "Matched keyword \"$keyword\" in visible UI content."
        } ?: "Monitored package appears on the high-risk app list."
    }

    private companion object {
        const val LogTag = "HaramVeilMode1"
    }
}
