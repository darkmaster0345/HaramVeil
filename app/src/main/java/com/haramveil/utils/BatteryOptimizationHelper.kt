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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.util.Locale

data class BatteryOptimizationGuide(
    val manufacturerLabel: String,
    val title: String,
    val summary: String,
    val steps: List<String>,
    val buttonLabel: String,
    val intent: Intent,
)

object BatteryOptimizationHelper {
    fun resolveGuide(
        context: Context,
    ): BatteryOptimizationGuide {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        return when {
            manufacturer.contains("samsung") -> samsungGuide(context)
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> xiaomiGuide(context)
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> huaweiGuide(context)
            else -> genericGuide(context)
        }
    }

    fun isIgnoringBatteryOptimizations(
        context: Context,
    ): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openGuide(
        context: Context,
        guide: BatteryOptimizationGuide,
    ) {
        context.startActivity(
            guide.intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun samsungGuide(
        context: Context,
    ): BatteryOptimizationGuide =
        BatteryOptimizationGuide(
            manufacturerLabel = "Samsung",
            title = "Set HaramVeil to Unrestricted on Samsung",
            summary = "One UI can quietly pause background monitoring unless HaramVeil is marked Unrestricted.",
            steps = listOf(
                "Open the HaramVeil app info page.",
                "Tap Battery.",
                "Choose Unrestricted.",
                "Return to HaramVeil and confirm you completed the step.",
            ),
            buttonLabel = "Open App Battery Path",
            intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )

    private fun xiaomiGuide(
        context: Context,
    ): BatteryOptimizationGuide =
        BatteryOptimizationGuide(
            manufacturerLabel = "Xiaomi / MIUI",
            title = "Enable AutoStart on Xiaomi / MIUI",
            summary = "MIUI often blocks services unless AutoStart and background activity are explicitly allowed.",
            steps = listOf(
                "Open the MIUI AutoStart settings page.",
                "Find HaramVeil and allow AutoStart.",
                "Also allow background activity if MIUI shows a battery prompt afterward.",
                "Return to HaramVeil and confirm the setup is done.",
            ),
            buttonLabel = "Open MIUI AutoStart",
            intent = firstResolvableIntent(
                context = context,
                candidates = listOf(
                    Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity",
                        )
                    },
                    Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.powercenter.PowerSettings",
                        )
                    },
                ),
                fallback = genericGuide(context).intent,
            ),
        )

    private fun huaweiGuide(
        context: Context,
    ): BatteryOptimizationGuide =
        BatteryOptimizationGuide(
            manufacturerLabel = "Huawei",
            title = "Allow Protected Apps on Huawei",
            summary = "Huawei can stop foreground monitoring unless HaramVeil is whitelisted in its startup and protected-app controls.",
            steps = listOf(
                "Open Huawei's startup or protected-app manager.",
                "Allow HaramVeil to auto-launch and stay running in the background.",
                "If Huawei shows a battery menu afterward, allow background activity there too.",
                "Return to HaramVeil and confirm the setup is complete.",
            ),
            buttonLabel = "Open Huawei Protection Settings",
            intent = firstResolvableIntent(
                context = context,
                candidates = listOf(
                    Intent().apply {
                        component = ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                        )
                    },
                    Intent().apply {
                        component = ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.process.ProtectActivity",
                        )
                    },
                ),
                fallback = genericGuide(context).intent,
            ),
        )

    private fun genericGuide(
        context: Context,
    ): BatteryOptimizationGuide {
        val requestIntent = if (!isIgnoringBatteryOptimizations(context)) {
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            )
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
        return BatteryOptimizationGuide(
            manufacturerLabel = Build.MANUFACTURER.replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase(Locale.getDefault()) else character.toString()
            },
            title = "Allow HaramVeil to stay active",
            summary = "Android may still pause long-running protection unless battery optimizations are relaxed for HaramVeil.",
            steps = listOf(
                "Open the battery optimization page.",
                "Find HaramVeil if Android shows a list.",
                "Allow the app to run without optimization limits.",
                "Return here and confirm the setup is complete.",
            ),
            buttonLabel = "Open Battery Optimization Settings",
            intent = requestIntent,
        )
    }

    private fun firstResolvableIntent(
        context: Context,
        candidates: List<Intent>,
        fallback: Intent,
    ): Intent =
        candidates.firstOrNull { intent ->
            intent.resolveActivity(context.packageManager) != null
        } ?: fallback
}
