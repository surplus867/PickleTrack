package com.example.shared.db

import android.content.Context
import com.squareup.sqldelight.db.SqlDriver

// Simple holder for application Context. Call `DatabaseFactory.provideContext(context)` from
// your Android Application class (recommended) before creating the database.
object DatabaseContextHolder {
    lateinit var context: Context
}

actual class DatabaseFactory {
    actual fun createDriver(): SqlDriver {
        // SQLDelight-generated Schema class is not available because the Gradle plugin
        // for SQLDelight wasn't applied in this project (code generation missing).
        // Provide a helpful runtime message so developers know how to fix it.
        throw IllegalStateException(
            "SQLDelight generated Schema not found. Apply the SQLDelight Gradle plugin and build the project to generate the database Schema (see shared/build.gradle.kts and your SQLDelight config)."
        )
    }
}

// Helper to set the Android Context from the Android app startup.
fun DatabaseFactory.provideContext(context: Context) {
    DatabaseContextHolder.context = context.applicationContext
}
