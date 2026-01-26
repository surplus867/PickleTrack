package com.example.shared.domain

import kotlinx.coroutines.flow.Flow
import com.example.shared.domain.PracticeSession
import com.example.shared.domain.DrillEntry

data class BasicStats(
    val totalSessions: Long,
    val minutesThisWeek: Long
)

// SessionDetail now contains the session and its drills to match repository usage
data class SessionDetail(
    val session: PracticeSession,
    val drills: List<DrillEntry>
)

interface SessionRepository {
    fun observeSessions(): Flow<List<PracticeSession>>
    suspend fun getSessionDetail(id: String): SessionDetail?
    suspend fun addSession(session: PracticeSession, drills: List<DrillEntry>)
    suspend fun deleteSession(id: String)
    suspend fun getBasicStats(weekStartMillis: Long, weekEndMillis: Long): BasicStats
}