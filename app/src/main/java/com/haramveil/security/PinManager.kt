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
import at.favre.lib.crypto.bcrypt.BCrypt

class PinManager(
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

    fun storePIN(pin: String) {
        val bcryptHash = BCrypt.withDefaults().hashToString(BcryptCost, pin.toCharArray())
        encryptedPreferences.edit()
            .putString(PinHashKey, bcryptHash)
            .putInt(PinFailedAttemptsKey, 0)
            .putLong(PinLockoutUntilKey, 0L)
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

    fun onWrongAttempt() {
        clearExpiredLockoutIfNeeded()
        if (isLockedOut()) {
            return
        }

        val updatedAttempts = encryptedPreferences.getInt(PinFailedAttemptsKey, 0) + 1
        val editor = encryptedPreferences.edit()

        if (updatedAttempts >= MaxFailedAttempts) {
            editor.putInt(PinFailedAttemptsKey, updatedAttempts)
            editor.putLong(PinLockoutUntilKey, System.currentTimeMillis() + LockoutDurationMs)
        } else {
            editor.putInt(PinFailedAttemptsKey, updatedAttempts)
        }

        editor.apply()
    }

    fun resetFailedAttempts() {
        encryptedPreferences.edit()
            .putInt(PinFailedAttemptsKey, 0)
            .putLong(PinLockoutUntilKey, 0L)
            .apply()
    }

    fun isLockedOut(): Boolean {
        clearExpiredLockoutIfNeeded()
        return encryptedPreferences.getLong(PinLockoutUntilKey, 0L) > System.currentTimeMillis()
    }

    fun getLockoutRemainingMs(): Long {
        clearExpiredLockoutIfNeeded()
        return (encryptedPreferences.getLong(PinLockoutUntilKey, 0L) - System.currentTimeMillis())
            .coerceAtLeast(0L)
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
        if (lockoutUntil == 0L || lockoutUntil > System.currentTimeMillis()) {
            return
        }

        encryptedPreferences.edit()
            .putInt(PinFailedAttemptsKey, 0)
            .putLong(PinLockoutUntilKey, 0L)
            .apply()
    }

    companion object {
        private const val PreferenceFileName = "haramveil_secure_state"
        private const val PinHashKey = "pin_bcrypt_hash"
        private const val PinFailedAttemptsKey = "pin_failed_attempts"
        private const val PinLockoutUntilKey = "pin_lockout_until_epoch_ms"
        private const val BcryptCost = 12
        private const val MaxFailedAttempts = 3
        private const val LockoutDurationMs = 20 * 60 * 1_000L
    }
}
