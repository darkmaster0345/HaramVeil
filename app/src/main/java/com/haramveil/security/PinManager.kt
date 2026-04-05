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
import android.os.SystemClock
import android.provider.Settings
import at.favre.lib.crypto.bcrypt.BCrypt
import com.haramveil.data.local.HaramVeilDatabase

class PinManager(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val encryptedPreferences by lazy {
        EncryptedPrefsFactory.create(applicationContext, PreferenceFileName)
    }

    fun storePIN(pin: String) {
        val previousHash = storedPinHashOrNull()
        val bcryptHash = BCrypt.withDefaults().hashToString(BcryptCost, pin.toCharArray())
        HaramVeilDatabase.rekeyIfNeeded(
            context = applicationContext,
            oldPinHash = previousHash,
            newPinHash = bcryptHash,
        )
        encryptedPreferences.edit()
            .putString(PinHashKey, bcryptHash)
            .putInt(PinFailedAttemptsKey, 0)
            .putLong(PinLockoutUntilKey, 0L)
            .putLong(PinLockoutElapsedUntilKey, 0L)
            .putInt(PinLockoutBootCountKey, 0)
            .apply()
    }

    fun verifyPIN(attempt: String): Boolean {
        clearExpiredLockoutIfNeeded()
        if (isLockedOut()) {
            return false
        }

        val storedHash = encryptedPreferences.getString(PinHashKey, null) ?: return false
        return BCrypt.verifyer()
            .verify(attempt.toCharArray(), storedHash)
            .verified
    }

    fun isSet(): Boolean = encryptedPreferences.contains(PinHashKey)

    fun storedPinHashOrNull(): String? = encryptedPreferences.getString(PinHashKey, null)

    fun onWrongAttempt() {
        clearExpiredLockoutIfNeeded()
        if (isLockedOut()) {
            return
        }

        val updatedAttempts = encryptedPreferences.getInt(PinFailedAttemptsKey, 0) + 1
        val editor = encryptedPreferences.edit()

        if (updatedAttempts >= MaxFailedAttempts) {
            val nowWallClockMs = System.currentTimeMillis()
            editor.putInt(PinFailedAttemptsKey, updatedAttempts)
            editor.putLong(PinLockoutUntilKey, nowWallClockMs + LockoutDurationMs)
            editor.putLong(PinLockoutElapsedUntilKey, SystemClock.elapsedRealtime() + LockoutDurationMs)
            editor.putInt(PinLockoutBootCountKey, currentBootCount())
        } else {
            editor.putInt(PinFailedAttemptsKey, updatedAttempts)
        }

        editor.apply()
    }

    fun resetFailedAttempts() {
        encryptedPreferences.edit()
            .putInt(PinFailedAttemptsKey, 0)
            .putLong(PinLockoutUntilKey, 0L)
            .putLong(PinLockoutElapsedUntilKey, 0L)
            .putInt(PinLockoutBootCountKey, 0)
            .apply()
    }

    fun isLockedOut(): Boolean {
        clearExpiredLockoutIfNeeded()
        return computeRemainingLockoutMs() > 0L
    }

    fun getLockoutRemainingMs(): Long {
        clearExpiredLockoutIfNeeded()
        return computeRemainingLockoutMs()
    }

    fun remainingAttemptsBeforeLockout(): Int {
        clearExpiredLockoutIfNeeded()
        if (isLockedOut()) {
            return 0
        }
        val failedAttempts = encryptedPreferences.getInt(PinFailedAttemptsKey, 0)
        return (MaxFailedAttempts - failedAttempts).coerceAtLeast(0)
    }

    private fun clearExpiredLockoutIfNeeded() {
        val lockoutUntil = encryptedPreferences.getLong(PinLockoutUntilKey, 0L)
        if (lockoutUntil == 0L || computeRemainingLockoutMs() > 0L) {
            return
        }

        encryptedPreferences.edit()
            .putInt(PinFailedAttemptsKey, 0)
            .putLong(PinLockoutUntilKey, 0L)
            .putLong(PinLockoutElapsedUntilKey, 0L)
            .putInt(PinLockoutBootCountKey, 0)
            .apply()
    }

    // Use elapsedRealtime during the same boot so wall-clock changes do not immediately clear lockout.
    private fun computeRemainingLockoutMs(): Long {
        val wallClockRemainingMs =
            encryptedPreferences.getLong(PinLockoutUntilKey, 0L) - System.currentTimeMillis()
        if (wallClockRemainingMs <= 0L) {
            val sameBootRemainingMs = remainingElapsedRealtimeLockoutMs()
            return sameBootRemainingMs.coerceAtLeast(0L)
        }

        val sameBootRemainingMs = remainingElapsedRealtimeLockoutMs()
        return maxOf(wallClockRemainingMs, sameBootRemainingMs, 0L)
    }

    private fun remainingElapsedRealtimeLockoutMs(): Long {
        val storedBootCount = encryptedPreferences.getInt(PinLockoutBootCountKey, 0)
        val currentBootCount = currentBootCount()
        if (storedBootCount == 0 || storedBootCount != currentBootCount) {
            return 0L
        }

        val elapsedLockoutUntil = encryptedPreferences.getLong(PinLockoutElapsedUntilKey, 0L)
        if (elapsedLockoutUntil == 0L) {
            return 0L
        }

        return elapsedLockoutUntil - SystemClock.elapsedRealtime()
    }

    private fun currentBootCount(): Int =
        runCatching {
            Settings.Global.getInt(applicationContext.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrDefault(0)

    companion object {
        private const val PreferenceFileName = "haramveil_secure_state"
        private const val PinHashKey = "pin_bcrypt_hash"
        private const val PinFailedAttemptsKey = "pin_failed_attempts"
        private const val PinLockoutUntilKey = "pin_lockout_until_epoch_ms"
        private const val PinLockoutElapsedUntilKey = "pin_lockout_until_elapsed_ms"
        private const val PinLockoutBootCountKey = "pin_lockout_boot_count"
        private const val BcryptCost = 12
        private const val MaxFailedAttempts = 3
        private const val LockoutDurationMs = 20 * 60 * 1_000L
    }
}
