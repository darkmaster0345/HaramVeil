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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class UITreeScannerTest {

    private val scanner = UITreeScanner()

    @Test
    fun scan_returnsHighRisk_whenVisibleTextContainsBlockedKeyword() {
        val rootNode = mock(AccessibilityNodeInfo::class.java)
        val childNode = mock(AccessibilityNodeInfo::class.java)

        `when`(rootNode.isVisibleToUser).thenReturn(true)
        `when`(rootNode.childCount).thenReturn(1)
        `when`(rootNode.getChild(0)).thenReturn(childNode)
        `when`(rootNode.text).thenReturn(null)
        `when`(rootNode.contentDescription).thenReturn(null)
        `when`(rootNode.viewIdResourceName).thenReturn("com.instagram.android:id/root")

        `when`(childNode.isVisibleToUser).thenReturn(true)
        `when`(childNode.childCount).thenReturn(0)
        `when`(childNode.text).thenReturn("Suggested NSFW content")
        `when`(childNode.contentDescription).thenReturn(null)
        `when`(childNode.viewIdResourceName).thenReturn("com.instagram.android:id/caption")

        val result = scanner.scan(
            rootNode = rootNode,
            packageName = "com.instagram.android",
            keywordBlocklist = listOf("nsfw"),
        )

        assertTrue(result.triggered)
        assertEquals("nsfw", result.matchedKeyword)
        assertEquals("com.instagram.android", result.packageName)
        assertEquals(RiskLevel.HIGH, result.riskLevel)
    }
}
