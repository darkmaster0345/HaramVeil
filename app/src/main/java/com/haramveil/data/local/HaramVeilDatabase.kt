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

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import com.haramveil.security.PinManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import java.time.Instant
import java.time.ZoneId

class HaramVeilDatabase private constructor(
    context: Context,
    passphrase: CharArray,
) {
    private val applicationContext = context.applicationContext
    private val helper = SqlCipherStatsOpenHelper(
        context = applicationContext,
        passphrase = passphrase,
    )
    private val blockEventDao = SqlCipherBlockEventDao(helper)

    fun blockEventDao(): BlockEventDao = blockEventDao

    fun rekey(
        newPinHash: String,
    ) {
        helper.rekey(StatsDatabasePassphrase.passphraseString(applicationContext, newPinHash))
    }

    companion object {
        private const val DatabaseName = "haramveil_stats.db"

        @Volatile
        private var instance: HaramVeilDatabase? = null

        fun getInstance(
            context: Context,
        ): HaramVeilDatabase =
            instance ?: synchronized(this) {
                val appContext = context.applicationContext
                val pinHash = PinManager(appContext).storedPinHashOrNull()
                    ?: error("Stats database requires an initialized PIN hash before opening.")
                instance ?: HaramVeilDatabase(
                    context = appContext,
                    passphrase = StatsDatabasePassphrase.passphraseChars(appContext, pinHash),
                ).also { database ->
                    instance = database
                }
            }

        fun rekeyIfNeeded(
            context: Context,
            oldPinHash: String?,
            newPinHash: String,
        ) {
            synchronized(this) {
                val appContext = context.applicationContext
                if (!appContext.getDatabasePath(DatabaseName).exists()) {
                    return
                }

                val activeInstance = instance
                if (activeInstance != null) {
                    activeInstance.rekey(newPinHash)
                    return
                }

                val temporaryDatabase = HaramVeilDatabase(
                    context = appContext,
                    passphrase = if (!oldPinHash.isNullOrBlank()) {
                        StatsDatabasePassphrase.passphraseChars(appContext, oldPinHash)
                    } else {
                        StatsDatabasePassphrase.legacyFallbackPassphraseChars(appContext)
                    },
                )
                temporaryDatabase.rekey(newPinHash)
            }
        }
    }
}

private class SqlCipherStatsOpenHelper(
    context: Context,
    private val passphrase: CharArray,
) : SQLiteOpenHelper(
    context,
    DatabaseName,
    null,
    DatabaseVersion,
) {
    init {
        SQLiteDatabase.loadLibs(context)
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS block_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                package_name TEXT NOT NULL,
                app_name TEXT NOT NULL,
                trigger_mode INTEGER NOT NULL,
                detection_detail TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                lockdown_duration_ms INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_block_events_timestamp ON block_events(timestamp DESC)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_block_events_mode ON block_events(trigger_mode)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_block_events_package ON block_events(package_name)",
        )
    }

    override fun onUpgrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        if (oldVersion != newVersion) {
            database.execSQL("DROP TABLE IF EXISTS block_events")
            onCreate(database)
        }
    }

    fun openReadable(): SQLiteDatabase = getReadableDatabase(passphrase)

    fun openWritable(): SQLiteDatabase = getWritableDatabase(passphrase)

    fun rekey(
        newPassphrase: String,
    ) {
        openWritable().changePassword(newPassphrase.toCharArray())
    }

    companion object {
        private const val DatabaseName = "haramveil_stats.db"
        private const val DatabaseVersion = 1
    }
}

private class SqlCipherBlockEventDao(
    private val helper: SqlCipherStatsOpenHelper,
) : BlockEventDao {
    private val eventsState = MutableStateFlow(loadEvents())

    override suspend fun insertEvent(event: BlockEvent) {
        val database = helper.openWritable()
        val values = ContentValues().apply {
            put("package_name", event.packageName)
            put("app_name", event.appName)
            put("trigger_mode", event.triggerMode)
            put("detection_detail", event.detectionDetail)
            put("timestamp", event.timestamp)
            put("lockdown_duration_ms", event.lockdownDurationMs)
        }
        database.insert("block_events", null, values)
        refresh()
    }

    override fun getAll(): Flow<List<BlockEvent>> = eventsState

    override fun getToday(): Flow<List<BlockEvent>> = eventsState.map { events ->
        val today = Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        events.filter { event ->
            Instant.ofEpochMilli(event.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate() == today
        }
    }

    override fun getThisWeek(): Flow<List<BlockEvent>> = eventsState.map { events ->
        val cutoffDate = Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .minusDays(6)
        events.filter { event ->
            !Instant.ofEpochMilli(event.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .isBefore(cutoffDate)
        }
    }

    override fun getMostBlockedApp(): Flow<String?> = eventsState.map { events ->
        mostBlockedRecord(events)?.appName
    }

    override fun getMostBlockedAppRecord(): Flow<MostBlockedAppRecord?> = eventsState.map(::mostBlockedRecord)

    override suspend fun deleteAll() {
        helper.openWritable().delete("block_events", null, null)
        refresh()
    }

    override fun getCountByMode(mode: Int): Flow<Int> = eventsState.map { events ->
        events.count { event -> event.triggerMode == mode }
    }

    override suspend fun deleteOlderThan(cutoffTimestampMs: Long): Int {
        val deletedRows = helper.openWritable().delete(
            "block_events",
            "timestamp < ?",
            arrayOf(cutoffTimestampMs.toString()),
        )
        refresh()
        return deletedRows
    }

    private fun refresh() {
        eventsState.value = loadEvents()
    }

    private fun loadEvents(): List<BlockEvent> {
        val database = helper.openReadable()
        return database.rawQuery(
            """
            SELECT id, package_name, app_name, trigger_mode, detection_detail, timestamp, lockdown_duration_ms
            FROM block_events
            ORDER BY timestamp DESC, id DESC
            """.trimIndent(),
            emptyArray(),
        ).useCursor { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        BlockEvent(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            packageName = cursor.getString(cursor.getColumnIndexOrThrow("package_name")),
                            appName = cursor.getString(cursor.getColumnIndexOrThrow("app_name")),
                            triggerMode = cursor.getInt(cursor.getColumnIndexOrThrow("trigger_mode")),
                            detectionDetail = cursor.getString(cursor.getColumnIndexOrThrow("detection_detail")),
                            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                            lockdownDurationMs = cursor.getLong(cursor.getColumnIndexOrThrow("lockdown_duration_ms")),
                        ),
                    )
                }
            }
        }
    }

    private fun mostBlockedRecord(
        events: List<BlockEvent>,
    ): MostBlockedAppRecord? {
        return events
            .groupBy { event -> event.packageName to event.appName }
            .maxWithOrNull(
                compareBy<Map.Entry<Pair<String, String>, List<BlockEvent>>> { entry -> entry.value.size }
                    .thenBy { entry -> entry.value.maxOfOrNull(BlockEvent::timestamp) ?: 0L },
            )
            ?.let { entry ->
                MostBlockedAppRecord(
                    packageName = entry.key.first,
                    appName = entry.key.second,
                    blockCount = entry.value.size,
                )
            }
    }
}

private inline fun <T> Cursor.useCursor(
    block: (Cursor) -> T,
): T {
    return try {
        block(this)
    } finally {
        try {
            close()
        } catch (_: SQLiteException) {
        }
    }
}
