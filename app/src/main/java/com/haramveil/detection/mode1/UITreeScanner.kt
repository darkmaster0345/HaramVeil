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

package com.haramveil.detection.mode1

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
}

data class ScanResult(
    val triggered: Boolean,
    val matchedKeyword: String?,
    val packageName: String,
    val riskLevel: RiskLevel,
)

class UITreeScanner {

    fun scan(
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        keywordBlocklist: List<String>,
    ): ScanResult {
        if (rootNode == null) {
            return ScanResult(
                triggered = false,
                matchedKeyword = null,
                packageName = packageName,
                riskLevel = RiskLevel.LOW,
            )
        }

        val extractedSignals = collectVisibleSignals(rootNode)
        val matchedKeyword = findMatchedKeyword(
            extractedSignals = extractedSignals,
            keywordBlocklist = keywordBlocklist,
        )
        val riskLevel = when {
            matchedKeyword != null -> RiskLevel.HIGH
            isHighRiskPackage(packageName) -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        return ScanResult(
            triggered = riskLevel != RiskLevel.LOW,
            matchedKeyword = matchedKeyword,
            packageName = packageName,
            riskLevel = riskLevel,
        )
    }

    fun computeNodeHash(rootNode: AccessibilityNodeInfo?): Int {
        if (rootNode == null) {
            return 0
        }
        val extractedSignals = collectVisibleSignals(rootNode)
        return buildString {
            append(rootNode.packageName?.toString().orEmpty())
            append('|')
            append(extractedSignals.visibleTexts.joinToString(separator = "|"))
            append('|')
            append(extractedSignals.resourceIds.joinToString(separator = "|"))
            append('|')
            append(extractedSignals.contentDescriptions.joinToString(separator = "|"))
        }.hashCode()
    }

    fun isHighRiskPackage(packageName: String): Boolean {
        val normalizedPackage = packageName.lowercase(Locale.ROOT)
        return normalizedPackage in HighRiskPackages ||
            normalizedPackage.contains("browser") ||
            normalizedPackage.contains("video")
    }

    private fun collectVisibleSignals(
        rootNode: AccessibilityNodeInfo,
    ): ExtractedUiSignals {
        val visibleTexts = linkedSetOf<String>()
        val resourceIds = linkedSetOf<String>()
        val contentDescriptions = linkedSetOf<String>()
        val traversalStack = ArrayDeque<AccessibilityNodeInfo>()
        traversalStack.addLast(rootNode)

        while (traversalStack.isNotEmpty()) {
            val currentNode = traversalStack.removeLast()
            if (currentNode.isVisibleToUser) {
                currentNode.text
                    ?.toString()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let(visibleTexts::add)

                currentNode.viewIdResourceName
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let(resourceIds::add)

                currentNode.contentDescription
                    ?.toString()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let(contentDescriptions::add)
            }

            for (childIndex in 0 until currentNode.childCount) {
                currentNode.getChild(childIndex)?.let { childNode ->
                    traversalStack.addLast(childNode)
                }
            }
        }

        return ExtractedUiSignals(
            visibleTexts = visibleTexts.toList(),
            resourceIds = resourceIds.toList(),
            contentDescriptions = contentDescriptions.toList(),
        )
    }

    private fun findMatchedKeyword(
        extractedSignals: ExtractedUiSignals,
        keywordBlocklist: List<String>,
    ): String? {
        if (keywordBlocklist.isEmpty()) {
            return null
        }

        val searchableSignals = buildList {
            addAll(extractedSignals.visibleTexts)
            addAll(extractedSignals.contentDescriptions)
            addAll(extractedSignals.resourceIds)
        }

        return keywordBlocklist.firstOrNull { entry ->
            val trimmedEntry = entry.trim()
            if (trimmedEntry.isEmpty()) {
                return@firstOrNull false
            }

            searchableSignals.any { signal ->
                if (looksLikeRegex(trimmedEntry)) {
                    runCatching {
                        Regex(trimmedEntry, RegexOption.IGNORE_CASE).containsMatchIn(signal)
                    }.getOrDefault(false)
                } else {
                    signal.contains(other = trimmedEntry, ignoreCase = true)
                }
            }
        }
    }

    private fun looksLikeRegex(entry: String): Boolean =
        entry.any { character -> character in RegexMetaCharacters }

    private companion object {
        val RegexMetaCharacters = setOf(
            '\\',
            '^',
            '$',
            '.',
            '|',
            '?',
            '*',
            '+',
            '(',
            ')',
            '[',
            ']',
            '{',
            '}',
        )

        val HighRiskPackages: Set<String> = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "com.snapchat.android",
            "com.twitter.android",
            "com.google.android.youtube",
            "com.reddit.frontpage",
            "com.opera.browser",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "org.telegram.messenger",
            "com.whatsapp",
        )
    }
}

private data class ExtractedUiSignals(
    val visibleTexts: List<String>,
    val resourceIds: List<String>,
    val contentDescriptions: List<String>,
)
