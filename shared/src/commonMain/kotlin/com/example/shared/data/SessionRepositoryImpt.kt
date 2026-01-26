package com.example.shared.data

import com.example.shared.db.PickleTrackDatabase
import com.example.shared.domain.BasicStats
import com.example.shared.domain.DrillEntry
import com.example.shared.domain.PracticeSession
import com.example.shared.domain.SessionDetail
import com.example.shared.domain.SessionRepository
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SQLDelight implementation of SessionRepository.
 *
 * Responsible for:
 * - Reading/writing practice sessions
 * - Mapping database rows to domain models
 * - Exposing reactive streams using Flow
 */
class SessionRepositoryImpl(
    private val db: PickleTrackDatabase
) : SessionRepository {

// Generated SQLDelight queries
    private val queries = db.pickletrackQueries

    /**
     * Observe all practice sessions.
     *
     * Emits a new list whenever the sessions table changes.
     * Ideal for Compose UI state collection.
     */
    override fun observeSessions(): Flow<List<PracticeSession>> {
        return queries.selectAllSessions()
            .asFlow()            // Convert SQL query to Flow
            .mapToList()         // Emit List when DB updates
            .map { rows ->
                rows.map {
                    PracticeSession(
                        id = it.id,
                        dateMillis = it.dateMillis,
                        durationMinutes = it.durationMinutes.toInt(),
                        location = it.location,
                        notes = it.notes
                    )
                }
            }
    }

    /**
     * Get a single session with its associated drills.
     *
     * Returns null if the session does not exist.
     */
    override suspend fun getSessionDetail(id: String): SessionDetail? {
        val s = queries.selectSessionById(id).executeAsOneOrNull() ?: return null
        val drills = queries.selectDrillsForSession(id).executeAsList().map {
            DrillEntry(
                id = it.id,
                sessionId = it.sessionId,
                name = it.name,
                rating = it.rating.toInt(),
                notes = it.notes
            )
        }
        return SessionDetail(
            session = PracticeSession(
                id = s.id,
                dateMillis = s.dateMillis,
                durationMinutes = s.durationMinutes.toInt(),
                location = s.location,
                notes = s.notes
            ),
            drills = drills
        )
    }

    /**
     * Insert a new practice session along with its drills.
     *
     * Wrapped ub a transaction to ensure atomicity.
     */
    override suspend fun addSession(session: PracticeSession, drills: List<DrillEntry>) {
        db.transaction {
            queries.insertSession(
                id = session.id,
                dateMillis = session.dateMillis,
                durationMinutes = session.durationMinutes.toLong(),
                location = session.location,
                notes = session.notes
            )
            drills.forEach { d ->
                queries.insertDrill(
                    id = d.id,
                    sessionId = d.sessionId,
                    name = d.name,
                    rating = d.rating.toLong(),
                    notes = d.notes
                )
            }
        }
    }

    /**
     * Delete a session by ID.
     * (Drills should be deleted via cascade or separate query.)
     */
    override suspend fun deleteSession(id: String) {
        queries.deleteSessionById(id)
    }

    /**
     * Get basic statistics for the dashboard.
     *
     * - Total sessions overall
     * - Total minutes within a data range
     */
    override suspend fun getBasicStats(weekStartMillis: Long, weekEndMillis: Long): BasicStats {
        val total = queries.totalSessionsCount().executeAsOne()
        val weekMinutes = queries.totalMinutesInRange(
            weekStartMillis,
            weekEndMillis
        ).executeAsOne()
        return BasicStats(
            totalSessions = total,
            minutesThisWeek = weekMinutes
        )
    }
}