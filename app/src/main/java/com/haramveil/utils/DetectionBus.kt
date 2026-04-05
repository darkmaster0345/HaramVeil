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

import com.haramveil.detection.mode1.ScanResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DetectionTriggerMode {
    MODE_1,
    MODE_2,
    MODE_3,
    LOCKDOWN,
}

data class Mode1WakeRequest(
    val scanResult: ScanResult,
    val wakeMode2: Boolean,
    val wakeMode3: Boolean,
    val issuedAtEpochMs: Long = System.currentTimeMillis(),
)

sealed interface DetectionEvent {
    data class Mode1Triggered(
        val trigger: Mode1WakeRequest,
    ) : DetectionEvent

    data class Mode2Triggered(
        val packageName: String,
        val matchDetails: String,
    ) : DetectionEvent

    data class Mode2Clear(
        val packageName: String,
        val details: String,
    ) : DetectionEvent

    data class Mode3Triggered(
        val packageName: String,
        val matchDetails: String,
    ) : DetectionEvent

    data class Mode3Clear(
        val packageName: String,
        val details: String,
    ) : DetectionEvent

    data class VeilRequested(
        val packageName: String,
        val triggerMode: DetectionTriggerMode,
        val matchDetails: String,
    ) : DetectionEvent

    data class WindowStateObserved(
        val packageName: String,
        val eventType: Int,
        val observedAtEpochMs: Long = System.currentTimeMillis(),
    ) : DetectionEvent
}

object DetectionBus {
    private val _mode1WakeRequests = MutableStateFlow<Mode1WakeRequest?>(null)
    private val _events = MutableSharedFlow<DetectionEvent>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val mode1WakeRequests: StateFlow<Mode1WakeRequest?> = _mode1WakeRequests.asStateFlow()
    val events: SharedFlow<DetectionEvent> = _events.asSharedFlow()

    fun publishMode1Triggered(trigger: Mode1WakeRequest) {
        _mode1WakeRequests.value = trigger
        _events.tryEmit(DetectionEvent.Mode1Triggered(trigger))
    }

    fun publishMode2Triggered(
        packageName: String,
        matchDetails: String,
    ) {
        _events.tryEmit(
            DetectionEvent.Mode2Triggered(
                packageName = packageName,
                matchDetails = matchDetails,
            ),
        )
    }

    fun publishMode2Clear(
        packageName: String,
        details: String,
    ) {
        _events.tryEmit(
            DetectionEvent.Mode2Clear(
                packageName = packageName,
                details = details,
            ),
        )
    }

    fun publishMode3Triggered(
        packageName: String,
        matchDetails: String,
    ) {
        _events.tryEmit(
            DetectionEvent.Mode3Triggered(
                packageName = packageName,
                matchDetails = matchDetails,
            ),
        )
    }

    fun publishMode3Clear(
        packageName: String,
        details: String,
    ) {
        _events.tryEmit(
            DetectionEvent.Mode3Clear(
                packageName = packageName,
                details = details,
            ),
        )
    }

    fun publishVeilRequested(
        packageName: String,
        triggerMode: DetectionTriggerMode,
        matchDetails: String,
    ) {
        _events.tryEmit(
            DetectionEvent.VeilRequested(
                packageName = packageName,
                triggerMode = triggerMode,
                matchDetails = matchDetails,
            ),
        )
    }

    fun publishWindowStateObserved(
        packageName: String,
        eventType: Int,
    ) {
        _events.tryEmit(
            DetectionEvent.WindowStateObserved(
                packageName = packageName,
                eventType = eventType,
            ),
        )
    }
}
