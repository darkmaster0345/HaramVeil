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

class PinSecurityStore(
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

    fun hasPin(): Boolean = encryptedPreferences.contains(PinHashKey)

    fun storePin(pin: String) {
        val bcryptHash = BCrypt.withDefaults().hashToString(BcryptCost, pin.toCharArray())
        encryptedPreferences.edit()
            .putString(PinHashKey, bcryptHash)
            .apply()
    }

    private companion object {
        const val PreferenceFileName = "haramveil_secure_state"
        const val PinHashKey = "pin_bcrypt_hash"
        const val BcryptCost = 12
    }
}
