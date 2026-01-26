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
        // Observe session list changes
        scope.launch {
            observeSessions().collect { list ->
                _state.update { it.copy(isLoading = false, sessions = list, error = null) }
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
            runCatching {
                val range = Time.currentWeekRange()
                getStats(range.startMillis, range.endMillis)
            }.onSuccess { stats ->
                _state.update { it.copy(stats = stats) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: "Unknown error") }
            }
        }
    }

    /**
     * Clears coroutines when ViewModel is no longer needed.
     */
    fun clear() {
        scope.cancel()
    }
}
