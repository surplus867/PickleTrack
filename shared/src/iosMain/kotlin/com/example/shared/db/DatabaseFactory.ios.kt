package com.example.shared.db

import com.squareup.sqldelight.db.SqlDriver

actual class DatabaseFactory {
    actual fun createDriver(): SqlDriver {
        throw IllegalStateException(
            "SQLDelight NativeSqliteDriver or generated Schema not found. Make sure SQLDelight code generation is enabled and the native driver dependency is configured in shared/build.gradle.kts."
        )
    }
}