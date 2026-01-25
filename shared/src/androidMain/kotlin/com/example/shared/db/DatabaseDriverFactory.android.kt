package com.example.shared.db

import com.example.shared.db.PickleTrackDatabase
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

// Use the DatabaseContextHolder helper in DatabaseFactory.android.kt to access application Context
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = PickleTrackDatabase.Schema,
            context = DatabaseContextHolder.context,
            name = "pickTrack.db"
        )
    }
}
