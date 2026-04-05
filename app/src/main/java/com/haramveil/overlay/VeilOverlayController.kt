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

package com.haramveil.overlay

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object VeilOverlayController {
    fun start(
        context: Context,
    ) {
        ContextCompat.startForegroundService(
            context.applicationContext,
            Intent(context.applicationContext, VeilOverlayService::class.java).apply {
                action = VeilOverlayService.ActionStart
            },
        )
    }

    fun stop(
        context: Context,
    ) {
        context.applicationContext.stopService(
            Intent(context.applicationContext, VeilOverlayService::class.java),
        )
    }
}
