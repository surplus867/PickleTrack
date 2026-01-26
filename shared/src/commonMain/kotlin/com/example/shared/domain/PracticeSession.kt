package com.example.shared.domain

import kotlin.Long

data class PracticeSession(
    val id: String,
    val dateMillis: Long,
    val durationMinutes: Int,
    val location: String?,
    val notes: String?
)

data class DrillEntry(
    val id: String,
    val sessionId: String,
    val name: String,
    val rating: Int, // 1..5
    val notes: String?
)