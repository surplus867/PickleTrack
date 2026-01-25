package com.example.shared.db

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver


actual class DatabaseFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = PickleTrackDatabase.Schema,
            context = context,
            name = "pickTrack.db"
        )
    }
}
