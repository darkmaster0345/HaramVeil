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

package com.haramveil.data.local

import android.content.Context
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object StatsDatabasePassphrase {
    private const val DerivedKeyLengthBytes = 32
    private const val HkdfAlgorithm = "HmacSHA256"
    private const val LegacyFallbackPinHash = "haramveil_stats_fallback_pin_hash_v1"
    private val InfoBytes = "haramveil.stats.block_events.v1".toByteArray(StandardCharsets.UTF_8)

    fun passphraseString(
        context: Context,
        pinHash: String,
    ): String {
        require(pinHash.isNotBlank()) {
            "Stats database passphrase requires a non-empty PIN hash."
        }
        return derivePassphraseString(
            context = context,
            inputKeyMaterial = pinHash.toByteArray(StandardCharsets.UTF_8),
        )
    }

    fun passphraseChars(
        context: Context,
        pinHash: String,
    ): CharArray = passphraseString(context, pinHash).toCharArray()

    // Kept only to rekey pre-release local test databases that were created before the fallback was removed.
    fun legacyFallbackPassphraseChars(
        context: Context,
    ): CharArray = derivePassphraseString(
        context = context,
        inputKeyMaterial = LegacyFallbackPinHash.toByteArray(StandardCharsets.UTF_8),
    ).toCharArray()

    private fun derivePassphraseString(
        context: Context,
        inputKeyMaterial: ByteArray,
    ): String {
        val salt = "${context.packageName}:haramveil_stats_room".toByteArray(StandardCharsets.UTF_8)
        val derivedKey = hkdfSha256(
            inputKeyMaterial = inputKeyMaterial,
            salt = salt,
            info = InfoBytes,
            length = DerivedKeyLengthBytes,
        )
        return derivedKey.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    private fun hkdfSha256(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int,
    ): ByteArray {
        val extractMac = Mac.getInstance(HkdfAlgorithm)
        extractMac.init(SecretKeySpec(salt, HkdfAlgorithm))
        val pseudoRandomKey = extractMac.doFinal(inputKeyMaterial)

        val output = ByteArray(length)
        val expandMac = Mac.getInstance(HkdfAlgorithm)
        expandMac.init(SecretKeySpec(pseudoRandomKey, HkdfAlgorithm))

        var previousBlock = ByteArray(0)
        var generatedBytes = 0
        var blockIndex = 1
        while (generatedBytes < length) {
            expandMac.reset()
            expandMac.update(previousBlock)
            expandMac.update(info)
            expandMac.update(blockIndex.toByte())
            previousBlock = expandMac.doFinal()

            val bytesToCopy = minOf(previousBlock.size, length - generatedBytes)
            System.arraycopy(previousBlock, 0, output, generatedBytes, bytesToCopy)
            generatedBytes += bytesToCopy
            blockIndex += 1
        }

        return output
    }
}
