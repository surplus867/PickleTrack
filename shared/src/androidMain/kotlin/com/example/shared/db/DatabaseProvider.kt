package com.example.shared.db

class DatabaseProvider(factory: DatabaseFactory) {
    val db: PickleTrackDatabase = PickleTrackDatabase(factory.createDriver())
}