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

package com.haramveil.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.haramveil.data.models.ActiveLockdown

class AppLockdownManager(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val encryptedPreferences by lazy {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            applicationContext,
            PreferenceFileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isLocked(
        packageName: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val expiry = encryptedPreferences.getLong(lockKey(packageName), 0L)
        return when {
            expiry <= 0L -> false
            expiry <= nowEpochMs -> {
                unlockApp(packageName)
                false
            }

            else -> true
        }
    }

    fun lockApp(
        packageName: String,
        durationMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ) {
        val expiryTimestamp = nowEpochMs + durationMs.coerceAtLeast(MinimumLockdownDurationMs)
        encryptedPreferences.edit()
            .putLong(lockKey(packageName), expiryTimestamp)
            .apply()
    }

    fun unlockApp(
        packageName: String,
    ) {
        encryptedPreferences.edit()
            .remove(lockKey(packageName))
            .apply()
    }

    fun activeLockdowns(
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<ActiveLockdown> {
        val expiredKeys = mutableListOf<String>()
        val activeEntries = encryptedPreferences.all.entries.mapNotNull { entry ->
            if (!entry.key.startsWith(LockPrefix)) {
                return@mapNotNull null
            }

            val expiryTimestamp = entry.value as? Long ?: return@mapNotNull null
            if (expiryTimestamp <= nowEpochMs) {
                expiredKeys += entry.key
                return@mapNotNull null
            }

            ActiveLockdown(
                packageName = entry.key.removePrefix(LockPrefix),
                expiresAtEpochMs = expiryTimestamp,
            )
        }.sortedBy { lockdown -> lockdown.expiresAtEpochMs }

        if (expiredKeys.isNotEmpty()) {
            encryptedPreferences.edit().apply {
                expiredKeys.forEach(::remove)
                apply()
            }
        }

        return activeEntries
    }

    private fun lockKey(packageName: String): String = "$LockPrefix$packageName"

    private companion object {
        const val PreferenceFileName = "haramveil_lockdowns"
        const val LockPrefix = "lockdown_"
        const val MinimumLockdownDurationMs = 60_000L
    }
}
