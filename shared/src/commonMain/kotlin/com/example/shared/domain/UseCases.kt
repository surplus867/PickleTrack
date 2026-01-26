package com.example.shared.domain

import kotlinx.coroutines.flow.Flow

/**
 * Use cases = small classes that perform ONE action.
 * They call the repository and are used by ViewModels.
 */

/**
 * Observes all practice sessions.
 * Emits a new list whenever the database changes.
 */
class ObserveSessionsUseCase(private val repo: SessionRepository) {
    /** Invoke to get a Flow that emits lists of [PracticeSession]. */
    // Simple: call this to start observing sessions
    operator fun invoke(): Flow<List<PracticeSession>> = repo.observeSessions()
}

/**
 * GetSessionDetailUseCase
 *
 * Fetches a single session and its related drills.
 * - Input: session id (String)
 * - Output: nullable [SessionDetail] (null if session not found)
 */
// Simple: fetches one session with its drills
class GetSessionDetailUseCase(private val repo: SessionRepository) {
    /** Invoke with a session id to load details (suspending). */
    // Simple: suspend and pass an id to get the details
    suspend operator fun invoke(id: String): SessionDetail? = repo.getSessionDetail(id)
}

/**
 * AddSessionUseCase
 *
 * Adds a new session and its drills in a single operation.
 * - Input: [PracticeSession] and list of [DrillEntry]
 * - Output: Unit (suspending)
 * - Important: repository implementation should perform inserts transactionally.
 */
// Simple: persist a session and its related drills
class AddSessionUseCase(private val repo: SessionRepository) {
    /** Invoke to persist a session and its drills. */
    // Simple: call this from a coroutine to save a session
    suspend operator fun invoke(session: PracticeSession, drills: List<DrillEntry>) {
        repo.addSession(session, drills)
    }
}

/**
 * DeleteSessionUseCase
 *
 * Deletes a session by id. Repository should cascade/delete related drills as appropriate.
 */
// Simple: delete a session by id
class DeleteSessionUseCase(private val repo: SessionRepository) {
    /** Invoke to delete the session with the provided id. */
    // Simple: suspend call to remove the session
    suspend operator fun invoke(id: String) = repo.deleteSession(id)
}

/**
 * GetBasicStatsUseCase
 *
 * Returns basic statistics (total sessions and minutes in a time range).
 * - Input: start and end millis for the week range
 * - Output: [BasicStats]
 */
// Simple: compute total sessions and minutes for a time window
class GetBasicStatsUseCase(private val repo: SessionRepository) {
    /** Invoke to compute basic stats for the provided time window. */
    // Simple: returns BasicStats for the given time range
    suspend operator fun invoke(weekStartMillis: Long, weekEndMillis: Long): BasicStats =
        repo.getBasicStats(weekStartMillis, weekEndMillis)
}