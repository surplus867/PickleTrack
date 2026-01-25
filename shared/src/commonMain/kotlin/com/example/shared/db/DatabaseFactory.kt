package com.example.shared.db

import com.squareup.sqldelight.db.SqlDriver

expect class DatabaseFactory {
    fun createDriver(): SqlDriver
}