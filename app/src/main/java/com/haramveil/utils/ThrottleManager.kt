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

package com.haramveil.utils

import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PendingAccessibilityEvent(
    val packageName: String,
    val eventType: Int,
)

class ThrottledScanTrigger(
    val packageName: String,
    val eventType: Int,
    val nodeHash: Int,
    val rootNode: AccessibilityNodeInfo,
    val issuedAtEpochMs: Long = System.currentTimeMillis(),
) {
    fun recycle() {
        rootNode.recycle()
    }
}

class ThrottleManager(
    scope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider(),
    initialDebounceMs: Long = 500L,
    private val buildTrigger: suspend (PendingAccessibilityEvent) -> ThrottledScanTrigger?,
) {
    private val debounceMs = MutableStateFlow(initialDebounceMs.coerceIn(minimumValue = 250L, maximumValue = 2_000L))
    private val pendingEvents = MutableSharedFlow<PendingAccessibilityEvent>(extraBufferCapacity = 32)
    private val _scanTriggers = MutableStateFlow<ThrottledScanTrigger?>(null)
    private var lastProcessedFingerprint: Pair<String, Int>? = null

    val scanTriggers = _scanTriggers.asStateFlow()

    init {
        scope.launch(dispatcherProvider.default) {
            pendingEvents.collectLatest { event ->
                delay(debounceMs.value)
                val trigger = buildTrigger(event) ?: return@collectLatest
                val fingerprint = event.packageName to trigger.nodeHash
                if (fingerprint == lastProcessedFingerprint) {
                    trigger.recycle()
                    return@collectLatest
                }

                lastProcessedFingerprint = fingerprint
                _scanTriggers.value = trigger
            }
        }
    }

    fun submit(
        packageName: String,
        eventType: Int,
    ) {
        pendingEvents.tryEmit(
            PendingAccessibilityEvent(
                packageName = packageName,
                eventType = eventType,
            ),
        )
    }

    fun updateDebounceMs(intervalMs: Long) {
        debounceMs.value = intervalMs.coerceIn(minimumValue = 250L, maximumValue = 2_000L)
    }
}
