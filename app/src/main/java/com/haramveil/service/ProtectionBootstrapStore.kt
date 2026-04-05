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

class ProtectionBootstrapStore(
    context: Context,
) {
    private val deviceProtectedContext = context.applicationContext.createDeviceProtectedStorageContext()
    private val preferences = deviceProtectedContext.getSharedPreferences(
        PreferenceFileName,
        Context.MODE_PRIVATE,
    )

    fun markOnboardingComplete() {
        preferences.edit()
            .putBoolean(BootstrapReadyKey, true)
            .apply()
    }

    fun isBootstrapReady(): Boolean =
        preferences.getBoolean(BootstrapReadyKey, false)

    fun setProtectionEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(ProtectionEnabledKey, enabled)
            .apply()
    }

    fun isProtectionEnabled(): Boolean =
        preferences.getBoolean(ProtectionEnabledKey, true)

    fun markServiceRunning(isRunning: Boolean) {
        preferences.edit()
            .putBoolean(ServiceRunningKey, isRunning)
            .apply()
    }

    fun updateServiceHeartbeat() {
        preferences.edit()
            .putLong(ServiceHeartbeatMsKey, System.currentTimeMillis())
            .apply()
    }

    fun isServiceAlive(
        maxStalenessMs: Long = DefaultHeartbeatStalenessMs,
    ): Boolean {
        if (!preferences.getBoolean(ServiceRunningKey, false)) {
            return false
        }
        val lastHeartbeat = preferences.getLong(ServiceHeartbeatMsKey, 0L)
        if (lastHeartbeat == 0L) {
            return false
        }
        return System.currentTimeMillis() - lastHeartbeat <= maxStalenessMs
    }

    fun recordTamperEvent(
        eventType: String,
        message: String,
        timestampMs: Long = System.currentTimeMillis(),
    ) {
        preferences.edit()
            .putString(LastTamperEventTypeKey, eventType)
            .putString(LastTamperEventMessageKey, message)
            .putLong(LastTamperEventTimestampMsKey, timestampMs)
            .apply()
    }

    fun lastTamperEvent(): TamperEventSnapshot? {
        val timestampMs = preferences.getLong(LastTamperEventTimestampMsKey, 0L)
        val message = preferences.getString(LastTamperEventMessageKey, null)
        val eventType = preferences.getString(LastTamperEventTypeKey, null)
        if (timestampMs == 0L || message.isNullOrBlank() || eventType.isNullOrBlank()) {
            return null
        }
        return TamperEventSnapshot(
            eventType = eventType,
            message = message,
            timestampMs = timestampMs,
        )
    }

    fun clearTamperEvent() {
        preferences.edit()
            .remove(LastTamperEventTypeKey)
            .remove(LastTamperEventMessageKey)
            .remove(LastTamperEventTimestampMsKey)
            .apply()
    }

    companion object {
        const val DefaultHeartbeatStalenessMs = 2 * 60 * 1_000L

        private const val PreferenceFileName = "haramveil_bootstrap_state"
        private const val BootstrapReadyKey = "bootstrap_ready"
        private const val ProtectionEnabledKey = "protection_enabled"
        private const val ServiceRunningKey = "service_running"
        private const val ServiceHeartbeatMsKey = "service_heartbeat_ms"
        private const val LastTamperEventTypeKey = "last_tamper_event_type"
        private const val LastTamperEventMessageKey = "last_tamper_event_message"
        private const val LastTamperEventTimestampMsKey = "last_tamper_event_timestamp_ms"
    }
}

data class TamperEventSnapshot(
    val eventType: String,
    val message: String,
    val timestampMs: Long,
)
