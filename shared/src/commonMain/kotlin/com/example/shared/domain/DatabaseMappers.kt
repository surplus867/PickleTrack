package com.example.shared.domain

import com.example.shared.db.Sessions
import com.example.shared.db.Drills

// Mapping extension functions from SQLDelight-generated types to domain models
fun Sessions.toDomain(): PracticeSession = PracticeSession(
    id = id,
    dateMillis = dateMillis,
    durationMinutes = durationMinutes.toInt(),
    location = location,
    notes = notes
)

fun Drills.toDomain(): DrillEntry = DrillEntry(
    id = id,
    sessionId = sessionId,
    name = name,
    rating = rating.toInt(),
    notes = notes
)

// Optional: helpers to map lists
fun List<Sessions>.toDomainList(): List<PracticeSession> = map { it.toDomain() }
fun List<Drills>.toDomainList(): List<DrillEntry> = map { it.toDomain() }
