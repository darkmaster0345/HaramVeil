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
import org.json.JSONArray

class VeilContentRepository(
    context: Context,
) {
    private val applicationContext = context.applicationContext

    @Volatile
    private var cachedEntries: List<VeilPassage>? = null

    fun randomPassage(): VeilPassage {
        val entries = loadEntries()
        return entries.randomOrNull() ?: fallbackPassage()
    }

    private fun loadEntries(): List<VeilPassage> {
        cachedEntries?.let { entries ->
            return entries
        }

        return synchronized(this) {
            cachedEntries?.let { entries ->
                return@synchronized entries
            }

            val parsedEntries = runCatching {
                applicationContext.assets.open(AssetFileName).bufferedReader().use { reader ->
                    val jsonArray = JSONArray(reader.readText())
                    buildList(jsonArray.length()) {
                        repeat(jsonArray.length()) { index ->
                            val item = jsonArray.getJSONObject(index)
                            add(
                                VeilPassage(
                                    id = item.getInt("id"),
                                    type = when (item.getString("type").lowercase()) {
                                        "ayah" -> VeilPassageType.AYAH
                                        else -> VeilPassageType.HADITH
                                    },
                                    arabic = item.getString("arabic"),
                                    english = item.getString("english"),
                                    source = item.getString("source"),
                                ),
                            )
                        }
                    }
                }
            }.getOrElse {
                listOf(fallbackPassage())
            }

            cachedEntries = parsedEntries
            parsedEntries
        }
    }

    private fun fallbackPassage(): VeilPassage =
        VeilPassage(
            id = 0,
            type = VeilPassageType.AYAH,
            arabic = "قُل لِّلْمُؤْمِنِينَ يَغُضُّوا مِنْ أَبْصَارِهِمْ",
            english = "Tell the believing men to lower their gaze and guard themselves.",
            source = "Quran 24:30",
        )

    private companion object {
        const val AssetFileName = "hadiths_ayahs.json"
    }
}
