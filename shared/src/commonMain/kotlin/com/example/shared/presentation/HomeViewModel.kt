package com.example.shared.presentation

import com.example.shared.domain.BasicStats
import com.example.shared.domain.GetBasicStatsUseCase
import com.example.shared.domain.ObserveSessionsUseCase
import com.example.shared.domain.PracticeSession
import com.example.shared.util.Time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Home screen.
 * Contains everything the screen needs to render.
 */
data class HomeState(
    val isLoading: Boolean = true,
    val sessions: List<PracticeSession> = emptyList(),
    val stats: BasicStats = BasicStats(totalSessions = 0, minutesThisWeek = 0),
    val error: String? = null
)

/**
 * ViewModel for the Home screen.
 * - Observes sessions
 * - Loads weekly stats
 * - Exposes state as StateFlow
 */
class HomeViewModel(
    private val observeSessions: ObserveSessionsUseCase,
    private val getStats: GetBasicStatsUseCase
) {

    // Coroutine scope owned by this ViewModel
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Internal mutable state
    private val _state = MutableStateFlow(HomeState())

    // Public read-only state for the UI
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /**
     * Starts observing sessions and loads stats.
     * Call this when the screen is shown.
     */
    fun start() {
        println("HomeViewModel: start() called")
        // Observe session list changes
        scope.launch {
            println("HomeViewModel: Starting to observe sessions")
            observeSessions().collect { list ->
                println("HomeViewModel: observeSessions emitted ${list.size} sessions")
                _state.update { it.copy(isLoading = false, sessions = list, error = null) }
                // Refresh stats whenever sessions change (e.g., after adding a new session)
                refreshStats()
            }
        }
        // Load stats once on start
        refreshStats()
    }

    /**
     * Loads stats for the current week and updates state.
     */
    private fun refreshStats() {
        scope.launch {
            println("HomeViewModel: refreshStats() called")
            runCatching {
                val range = Time.currentWeekRange()
                println("HomeViewModel: Getting stats for range ${range.startMillis} to ${range.endMillis}")
                getStats(range.startMillis, range.endMillis)
            }.onSuccess { stats ->
                println("HomeViewModel: Stats loaded - totalSessions=${stats.totalSessions}, minutesThisWeek=${stats.minutesThisWeek}")
                _state.update { it.copy(stats = stats) }
            }.onFailure { e ->
                println("HomeViewModel: Stats failed - ${e.message}")
                _state.update { it.copy(error = e.message ?: "Unknown error") }
            }
        }
    }

    /**
     * Clears coroutines when ViewModel is no longer needed.
     */
    fun clear() {
        println("HomeViewModel: clear() called")
        scope.cancel()
    }
}
