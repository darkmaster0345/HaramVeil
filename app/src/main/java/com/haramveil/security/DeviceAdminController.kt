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

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object DeviceAdminController {
    private const val DefaultExplanation =
        "HaramVeil uses Device Admin for uninstall resistance and stronger tamper protection after a block."

    fun componentName(
        context: Context,
    ): ComponentName = ComponentName(context, HaramVeilDeviceAdminReceiver::class.java)

    fun isEnabled(
        context: Context,
    ): Boolean {
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        return devicePolicyManager?.isAdminActive(componentName(context)) == true
    }

    fun buildEnableIntent(
        context: Context,
        explanation: String = DefaultExplanation,
    ): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun openEnablePrompt(
        context: Context,
        explanation: String = DefaultExplanation,
    ) {
        val standardIntent = buildEnableIntent(context, explanation)
        try {
            context.startActivity(standardIntent)
        } catch (_: Exception) {
            // Samsung One UI may reject the standard Device Admin intent.
            // Fall back to the general Security Settings screen.
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            } catch (_: Exception) {
                // Last resort: open the device's general settings page.
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        }
    }
}
