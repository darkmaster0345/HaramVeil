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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

object DebugLog {
    inline fun d(
        context: Context,
        tag: String,
        message: () -> String,
    ) {
        if (isDebuggable(context)) {
            Log.d(tag, message())
        }
    }

    inline fun i(
        context: Context,
        tag: String,
        message: () -> String,
    ) {
        if (isDebuggable(context)) {
            Log.i(tag, message())
        }
    }

    inline fun w(
        context: Context,
        tag: String,
        message: () -> String,
    ) {
        if (isDebuggable(context)) {
            Log.w(tag, message())
        }
    }

    fun isDebuggable(
        context: Context,
    ): Boolean = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
